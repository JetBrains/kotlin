# `ObjectClassLowering`

Gives every `object` and `companion object` a static field holding its single instance plus a static getter, and replaces
every object access (`IrGetObjectValue`) with a call to that getter. When an object provably has no state and no
side effects, its instance is a compile-time constant; otherwise it is created by code that the next lowering,
[`StaticInitializersLowering`](StaticInitializersLowering.md), turns into lazy initialization.

| | |
|---|---|
| Source | [ObjectClassLowering.kt](../../../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/ObjectClassLowering.kt) |
| Stage | second stage, after inlining (`getLoweringsAfterInlining`): `CoroutinesLivenessAnalysisFallback` → `ExpressionBodyTransformer` → **`ObjectClassLowering`** → `StaticInitializersLowering` |
| Runs | always |
| Kind | file lowering, Native-specific; takes `NativeGenerationState` because it needs the class layout (`getLayoutBuilder(...).getFields(generationState.llvm)`) |
| Prerequisites | none declared |

## What it does

- For an `object` other than a companion and other than `Unit`, it adds a property `$instance` to the object class itself.
- For a `companion object`, it adds a property `$companion` to the **outer** class:
  - with `newCompanionInitializationEnabled` on the outer class's klib, right after the companion (`companionIndex + 1`);
  - otherwise at the end of the outer class.
- Each of these properties has a `final`, `static` backing field and a getter without a dispatch receiver that returns the
  field. The field initializer is one of three shapes (see below).
- `IrGetObjectValue` becomes a call to that getter, with two exceptions:
  - `Unit` becomes `theUnitInstance()`;
  - the companion of an external ObjC class becomes `interopInterpretObjCPointer<C.Companion>(interopGetObjCClass<C>())`.
- If the object is annotated `@ThreadLocal`, the annotation is copied onto the new property. Storage is decided by property
  annotations (`IrField.storageKind`), so the field becomes thread-local.

## Example

Abridged from a `-Xphases-to-dump-before/after=ObjectClassLowering` dump. In the dump, `companion` marks static members, and the
constant object is printed as `error("")` because the dumper cannot render `IrConstantObject`.

```kotlin
// before
object Plain { val y = 1 }
object OnlyConst { const val x = 1 }
object WithInit { init { println("init") } }
@ThreadLocal object PerThread { const val z = 2 }
class Host { companion object { const val c = 3 } }

fun main() {
    println(Plain.y + OnlyConst.x + PerThread.z + Host.c)   // the const reads are already folded to 1, 2, 3
    println(WithInit)
    val u = Unit
}
```

```kotlin
// after
object Plain {
    val y = 1
    static val `$instance`: Plain
        field = { #$instance = createUninitializedInstance<Plain>()      // published before the constructor runs
                  initInstance(#$instance, Plain())
                  #$instance }
        static get() = #$instance
}
object OnlyConst {
    const val x = 1
    static val `$instance`: OnlyConst
        field = <IrConstantObject OnlyConst()>                            // no runtime initialization at all
        static get() = #$instance
}
object WithInit {   /* same shape as Plain: the init block disqualifies the constant path */ }
object PerThread {
    @ThreadLocal static val `$instance`: PerThread                       // annotation copied from the object
        field = { /* same shape as Plain: thread-local objects are never constant */ }
        static get() = #$instance
}
class Host {
    companion object { const val c = 3 }
    static val `$companion`: Companion                                   // in the outer class
        field = <IrConstantObject Companion()>
        static get() = #$companion
}

fun main() {
    println(`<get-$instance>`().y + 1 + 2 + 3)                           // Plain.y
    println(`<get-$instance>`())                                          // WithInit
    val u = theUnitInstance()
}
```

## How it works

`lower` runs an `IrBuildingTransformer` over the file. `visitClass` first transforms the class's children, so nested objects
are processed first. It then:
1. calls `processObjectClass(declaration, declaration)` if the class is an object, not a companion, and not `Unit`;
2. if the class has a companion, calls `processObjectClass(companion, declaration, addAtIndex)`.

`processObjectClass`:
1. Gets or creates the getter through `getObjectClassInstanceFunction`, which caches it in an `irAttribute` on the object
   class. It adds the getter's property to the target class and adds the backing field.
2. `require`s that the primary constructor has no parameters.
3. Chooses the initializer:
   - **Companion of an ObjC class** (`classToAdd.isObjCClass()`, which includes Kotlin subclasses of ObjC classes): the same
     `interopInterpretObjCPointer(interopGetObjCClass())` expression.
   - **Constant:** `irConstantObject(declaration, emptyList())` if `hasConstStateAndNoSideEffects()` and the object is not
     `@ThreadLocal`. `hasConstStateAndNoSideEffects()` holds when either:
     - the class is annotated `@kotlin.native.internal.CanBePrecreated`; or
     - every field of its layout has `isConst` (its property is a `const val`), and every constructor is an
       "autogenerated simple constructor": primary, no parameters, `Any()` delegation first, then only `INITIALIZE_FIELD` blocks.
   - **Otherwise:** a block that stores `createUninitializedInstance<T>()` into the field (origin `FIELD_PRE_INIT`), then calls
     `initInstance(field, T())` and returns the field. The code comment says the instance must be "available for rereading from
     the same thread while initializing".
4. Copies `@ThreadLocal` and gives the getter the body `return field`.

## Gotchas and implicit contracts

- **The getter is the object's access path throughout the backend.** `getObjectClassInstanceFunction` is also called by:
  - `RTTIGenerator`, for associated objects;
  - `ObjCExportCodeGenerator`;
  - the C adapter (`CAdapterCodegen`);
  - `DevirtualizationAnalysis`, for associated-object roots;
  - `CacheInfoBuilder`, which records object accesses for cache dependencies.

  It creates the getter on demand, so an object in another file or library gets the same `$instance`/`$companion` getter
  symbol before or without its own file being lowered. Inferred: the getter name is part of the cross-module and cache ABI.
- **Static getter, so initialization hooks attach to it.** The getter has no dispatch receiver, so `StaticInitializersLowering`
  prepends the lazy `$init_global` call to it. That is what makes the first access initialize the object. The lowering must
  run before `StaticInitializersLowering`; the phase list fixes that order, but there is no `@PhasePrerequisites` for it.
- **Constant objects depend on `InitializersLowering`'s output shape.** `isAutogeneratedSimpleConstructor` accepts only
  `INITIALIZE_FIELD` blocks after the `Any()` call. An `init {}` block, a non-`const` property with an initializer, or a
  superclass other than `Any` all fall back to runtime initialization, even when the values are constant (`Plain` above).
- **Published before construction.** In the runtime path the field holds the uninitialized instance while the constructor runs,
  so a re-entrant read on the same thread sees a partially initialized object instead of recursing.
- **`FIELD_PRE_INIT` has no reader.** `IrStatementOriginFieldPreInit` (also used by `EnumClassLowering` for `$VALUES`) is
  described as "a hack to avoid early freezing" and is not checked anywhere in this checkout.
- **Primary constructor only.** `declaration.constructors.single { it.isPrimary }` plus the empty-parameters `require`. An object
  without exactly one parameterless primary constructor fails here.
- **Companion placement follows the klib.** With `new_companion_initialization` (written when `LanguageFeature.CompanionBlocks`
  is on) the `$companion` field sits next to the companion, so declaration-order initialization matches program order.
  Without it, the field goes last.
- **External ObjC companions get no field.** `visitGetObjectValue` handles them inline, and `getObjectClassInstanceFunction`
  `require`s that it is never asked for one ("External objc Classes can't be used this way").
- **Reads of `const` members do not touch the object.** They are already folded before this lowering (see the example), so
  they never trigger initialization.

## Tests

- `native/native.tests/testData/codegen/objectDeclaration/globalConstants_*.kt` (constant objects).
- `compiler/testData/codegen/box/objects/` (object semantics in general).

## Related

- [StaticInitializersLowering](StaticInitializersLowering.md) turns the field initializers created here into lazy or eager
  initialization.
- [static-initializers.md](../static-initializers.md), section "Objects, companions, enums, instance initializers".
- [lowerings.md](../lowerings.md): pipeline position.
