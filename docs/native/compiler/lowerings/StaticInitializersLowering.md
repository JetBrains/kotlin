# `StaticInitializersLowering`

Moves the initializers of static fields (top-level properties, `$instance`/`$companion` fields of objects, `$VALUES` of enums,
…) out of their declarations into synthesized initializer functions, and makes every static entry point of the container call
them first. This is what turns "initialize a file's globals on first use" into explicit IR that codegen can compile into a
fast-path check. The full story, including codegen and the runtime state machine, is in
[static-initializers.md](../static-initializers.md); this page is the lowering-level reference.

| | |
|---|---|
| Source | [StaticInitializersLowering.kt](../../../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/StaticInitializersLowering.kt) |
| Stage | second stage, after inlining (`getLoweringsAfterInlining`): `ExpressionBodyTransformer` → `ObjectClassLowering` → **`StaticInitializersLowering`** → `ComputeTypesPass` (2nd run) |
| Runs | always |
| Kind | file lowering, Native-specific |
| Prerequisites | `ExpressionBodyTransformer` |

## What it does

For every container (the file, then each class, nested classes included) the lowering:

1. Sorts each static field with an initializer into one of four lists: lazy global, lazy thread-local, eager global, eager
   thread-local. It then clears `irField.initializer`.
2. Creates one private `Unit` function per non-empty list and inserts it at index 0 of the container:

   | List | Function name | Origin |
   |---|---|---|
   | lazy global | `$init_global` | `STATIC_GLOBAL_INITIALIZER` |
   | lazy thread-local | `$init_thread_local` | `STATIC_THREAD_LOCAL_INITIALIZER`, or `STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER` when the container has no lazy global initializer |
   | eager global | `$init_global_eager` | `EAGER_STATIC_GLOBAL_INITIALIZER` |
   | eager thread-local | `$init_thread_local_eager` | `EAGER_STATIC_THREAD_LOCAL_INITIALIZER` |

   In a file, the name gets the file name appended, e.g. `$init_global$a.kt`. The comment in `buildInitFunction` explains why:
   it avoids same-named private top-level functions in one package. In a class, the class fq name is already part of the
   mangled name. The body is the list of `irSetField` statements, with origin `INITIALIZE_GLOBAL_FIELD` or
   `INITIALIZE_THREAD_LOCAL_FIELD`.
3. Records the reference-typed fields of each list in the `initializedGlobals` attribute of its function.
4. If a lazy initializer exists, prepends calls (global first, then thread-local) to every function of the container that has
   no dispatch receiver, including property accessors, and to every constructor of a class.
5. Adds a public `$clinit_trigger(): Unit` to **every** class, interfaces and companion objects included. In a class with lazy
   initializers it gets the same prepended calls; otherwise it stays empty.

Eager initializers get no inserted calls. Codegen calls them from the module's runtime initializer instead.

## Example

Input (`a.kt`):

```kotlin
package demo

val greeting = "hi"                          // constant, final
var counter = 0                              // primitive constant
val list = listOf(1, 2)                      // needs code
@ThreadLocal val perThread = mutableListOf<Int>()
@EagerInitialization val eager = Any()

fun use() = list.size + perThread.size

open class Base { companion object { val b = Any() } }
```

After the lowering (abridged from a `-Xphases-to-dump-after=StaticInitializersLowering` dump; inlined bodies simplified):

```kotlin
private fun `$init_global_eager$a.kt`() { #eager = Any() }
private fun `$init_thread_local$a.kt`() { #perThread = ArrayList<Any?>() }
private fun `$init_global$a.kt`()       { #list = listOf(1, 2) }

val greeting = "hi"          // untouched: constant initializer stays on the field
var counter = 0              // untouched
val list: List<Int>          // initializer moved out
@ThreadLocal val perThread: MutableList<Int>
@EagerInitialization val eager: Any

fun use(): Int {
    `$init_global$a.kt`()            // inserted
    `$init_thread_local$a.kt`()      // inserted
    return list.size + perThread.size
}

open class Base {
    private static fun `$init_global`() {
        // the `$companion` field was created by ObjectClassLowering
        #$companion = { val c = createUninitializedInstance<Companion>(); initInstance(c, Companion()); c }
    }
    constructor() { `$init_global`(); super() }                        // inserted into constructors
    static val `$companion`: Companion get() { `$init_global`(); return #$companion }
    static fun `$clinit_trigger`() { `$init_global`() }
    companion object Companion {
        // `b` is an instance field of the companion, initialized in its constructor, not here
        static fun `$clinit_trigger`() { }                             // added to every class
    }
}
```

`static` marks functions with no dispatch receiver; the KotlinLike IR dumper prints them as `companion fun`.

With the klib flag `new_companion_initialization`, written when `LanguageFeature.CompanionBlocks` is enabled, a subclass first
triggers its superclass. This was checked with `-XXLanguage:+CompanionBlocks`:

```kotlin
class Derived : Base() { companion object { val d = Any() } }

// after
class Derived : Base {
    private static fun `$init_thread_local`() { Base.`$clinit_trigger`() }   // exists only because of the trigger
    private static fun `$init_global`() {
        Base.`$clinit_trigger`()
        #$companion = …
    }
    constructor() { `$init_global`(); `$init_thread_local`(); super() }
    …
}
```

## How it works

`lower` visits the file and every class (`visitFile`/`visitClass`). For a class it first adds `$clinit_trigger`, then runs
`processDeclarationContainter` (sic), and then recurses into nested declarations.

**Superclass triggers.** This only happens when the container is a non-interface class whose module's klib has
`newCompanionInitializationEnabled`. It follows JVMS §5.5, which the code quotes. It collects:
- the superclass other than `Any`, unless it is an external ObjC class (KT-90390 TODO);
- all superinterfaces, recursively and in declaration order, that declare a non-fake-override, non-abstract, non-static member
  function or property and are not external ObjC classes.

For each of them it puts a `null to irCall(superClass.$clinit_trigger)` entry into **both** the lazy global and the lazy
thread-local lists.

**Field classification.** It looks at each `IrField` declaration and each property's `backingField` that `isStatic`:
- **Skipped (left as is):** the initializer `isConst()` and the field is not thread-local, and the field is either non-reference
  (`!binaryTypeIsReference()`) or `final`.
  - `isConst()` accepts `IrConst`, `IrConstantValue`, and blocks of empty `IrComposite`s ending in a constant. Those empty
    composites are left over from local-declaration lowering.
  - Codegen emits such fields as static data.
- **Skipped:** no initializer and a primitive type. Nothing needs to be registered.
- **Thread-local:** `irField.storageKind == FieldStorageKind.THREAD_LOCAL`, i.e. `@ThreadLocal`.
- **Eager:** `shouldBeInitializedEagerly`, which is true in either of two cases:
  - the property is `@EagerInitialization`;
  - the field is top-level and lazy file initialization is disabled (`-Xir-property-lazy-initialization=disable`, which warns
    that eager initialization is deprecated).
- **Otherwise lazy.** A field without an initializer that is a reference type still goes into the list with a `null`
  expression, so that it appears in `initializedGlobals`.

**Insertion.** `addInitializersCall` returns early when there is no lazy initializer. It skips three kinds of function:
- functions with a dispatch receiver, which are "already initialized when instance was created";
- the entry point (`DECLARATION_ORIGIN_ENTRY_POINT`);
- the initializers themselves.

## Gotchas and implicit contracts

- **Bodies must be `IrBlockBody`.** `addInitializersCall` casts unconditionally, which is why `ExpressionBodyTransformer` is a
  prerequisite.
- **Order: global before thread-local.** The lowering inserts the global call first. In codegen
  (`evaluateFileThreadLocalInitializerCall`), a thread-local initializer whose container also has a global initializer is
  skipped while the global state is not `FILE_INITIALIZED`. The `STANDALONE` origin exists so that a container with only
  thread-local state does not check a global state word it doesn't have (`evaluateFileStandaloneThreadLocalInitializerCall`).
- **The calls are not ordinary calls.** `evaluateFunctionCall` recognizes the lazy origins. It compiles each call into an
  acquire load of the container's state word plus `CallInitGlobalPossiblyLock` or `CallInitThreadLocal`. It does not call the
  function directly. Changing the origins breaks this silently.
- **At most one of each per container.** `handleStaticInitializer` in `IrToBitcode.kt` `require`s it. Do not run this lowering
  twice on the same container.
- **`initializedGlobals` contract** (KDoc on the attribute): none of the listed fields may be accessed anywhere before the
  function starts. Codegen registers the global (non-thread-local) ones with `RegisterGlobal` at the start of the body
  (`handleStaticInitializerBody`). Primitive fields are deliberately left out.
- **Objects and enums depend on running after `ObjectClassLowering` and `EnumClassLowering`.** Their static
  `$instance`/`$companion`/`$VALUES` fields and static getters must already exist. The static getter of a lazily initialized
  field is a function without a dispatch receiver, so it gets the initializer call, and that is what makes `Base.Companion` or
  an object access trigger initialization.
- **The companion's own properties are instance fields.** `Companion.b` above is initialized by the companion's constructor.
  Only the `$companion` field in the outer class is static.
- **`$clinit_trigger` is created lazily** through the `clinitTriggerFunction` attribute. A subclass in another file can request
  its superclass's trigger before that file is lowered; `visitClass` reuses the same symbol later.
- **Later phases key on these origins:**
  - `RemoveRedundantCallsToStaticInitializersPhase` (`StaticInitializersOptimization.kt`) removes provably redundant calls.
    It finds the inserted calls only among the **first two statements** of a body ("the very first statements by
    construction"), and `calledInitializer` reads statement 0. A later lowering that puts anything before them hides them
    from the optimization.
  - `PreCodegenInliner` never inlines the initializers.
  - Devirtualization treats eager initializers as roots.
  - The DFG marks them `IS_EAGER_STATIC_INITIALIZER`.
- **New companion initialization is per klib.** It reads the `new_companion_initialization` manifest property of the
  declaration's own klib, so mixing klibs built with and without it changes initialization order per module. `CacheBuilder`
  rebuilds a cache when that property differs.

## Tests

- `native/native.tests/testData/codegen/initializers/`: `gcRegistration*.kt` (registration of globals, eager and thread-local
  variants), `files/globalInitedBeforeThreadLocal.kt`, `files/eagerInitialization/`, `files/workers*.kt`, and the
  `static_*.kt` cases (fields that stay constant).
- `compiler/testData/codegen/box/companionBlocksAndExtensions/` covers companion initialization with `CompanionBlocks`.

## Related

- [static-initializers.md](../static-initializers.md): codegen state words, the runtime state machine, eager initialization and
  caches, redundant-call removal.
- [lowerings.md](../lowerings.md): pipeline position.
- [ObjectClassLowering](ObjectClassLowering.md) and [EnumClassLowering](EnumClassLowering.md) create the static fields that this lowering initializes.
- `ExpressionBodyTransformer` is the prerequisite.
