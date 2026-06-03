# JVM Lowerings Explained

This document is a companion to [`JvmLoweringPhases.kt`](./JvmLoweringPhases.kt). It walks
through every lowering in the JVM IR pipeline in declaration order and gives, for each one,
a short description plus a tiny Kotlin "before / after" sketch that shows the *shape* of the
transformation.

A few caveats:

- The "after" snippets are illustrative Kotlin, **not** literal IR dumps. Real IR is a tree of
  nodes (calls, gets, blocks, etc.); the snippets describe the desugared shape of what the
  backend will eventually emit, not the exact IR node structure.
- Some lowerings only attach IR metadata, run validation, or invent names — they do not
  rewrite expressions. Those are tagged "_metadata only_".
- Comments inside `JvmLoweringPhases.kt` (about phase ordering, fragile invariants, etc.) are
  preserved as inline notes here.

The pipeline is split into three lists:

1. **Module phases (pre-file)** — `jvmModulePhases1`, run once over the whole module before any
   per-file work.
2. **File phases** — `jvmFilePhases`, run per `IrFile`. This is the bulk of the pipeline.
3. **Module phases (post-file)** — `jvmModulePhases2`, run once over the whole module after all
   file phases finish.

---

## 1. Module phases — pre-file (`jvmModulePhases1`)

### JvmUpgradeCallableReferences

Upgrades plain function/property reference and SAM-conversion IR nodes into the richer
`IrRichFunctionReference` / `IrRichPropertyReference` form used by later JVM phases. Carries
extra info such as bound receivers and overridden symbols.

_Metadata only — replaces one IR node kind with a more detailed one; no source-level rewrite._

### ExternalPackageParentPatcherLowering

Re-parents top-level callables that were deserialized from other modules so that their
`parent` points at the appropriate file-class / multifile facade rather than at the synthetic
`IrExternalPackageFragment`. Important for K2/FIR builds.

_Metadata only — fixes up parent pointers on deserialized symbols._

### FragmentSharedVariablesLowering

For debugger expression-evaluation fragments, promotes parameters tagged as "shared" into
mutable reference wrappers (e.g. `IntRef`) so the fragment can write back to outer-frame
locals.

```kotlin
// Before — fragment receives raw locals
fun fragment(x: Int) { /* ... */ }

// After (conceptual) — captured locals are wrapped in Ref types
fun fragment(x: Ref.IntRef) { /* x.element = ... */ }
```

### JvmK1IrValidationBeforeLoweringPhase

Validates the IR shape before any lowerings run. Active only on the K1 frontend; on K2/FIR
the IR has already been validated upstream so this is a no-op.

_Metadata only — runs `IrValidator`; no transformation._

### ProcessOptionalAnnotations

Records metadata about `@OptionalExpectation`-annotated annotation classes so they are written
into the `.kotlin_module` file. Used by multiplatform compilation.

_Metadata only — registers entries on the JVM context; no IR rewrite._

### JvmExpectDeclarationRemover

Drops `expect` declarations from the module so they do not reach codegen. K2 simply filters
them; K1 delegates to the common `ExpectDeclarationRemover`.

```kotlin
// Before — common module
expect fun foo()
// Platform module
actual fun foo() { /* ... */ }

// After — only the actual remains
fun foo() { /* ... */ }
```

### ConstEvaluationLowering

Evaluates calls to functions annotated with `@kotlin.internal.IntrinsicConstEvaluation`
(typically stdlib intrinsics) at compile time using the IR interpreter, and replaces them
with the resulting constant.

```kotlin
// Before
val s = "hello".uppercase()         // uppercase() is @IntrinsicConstEvaluation

// After (conceptual)
val s = "HELLO"
```

### FileClassLowering

Wraps every top-level function and property in a synthetic file class (e.g. `FooKt`). Honors
`@JvmName`, `@JvmPackageName` and `@JvmMultifileClass` on the file. Files with no top-level
members are skipped.

```kotlin
// Before — file Foo.kt
fun greet() = "hi"
val pi = 3.14

// After (conceptual) — wrapped in a synthetic class
class FooKt {
    companion object { /* statics */ }
    /* static */ fun greet() = "hi"
    /* static */ val pi = 3.14
}
```

### JvmStaticInObjectLowering

For `@JvmStatic` members of plain (non-companion) `object`s: removes the dispatch receiver and
rewrites every call site to a static-style call. Companion-object `@JvmStatic` is handled
later by `JvmStaticInCompanionLowering`.

```kotlin
// Before
object Singleton {
    @JvmStatic fun foo() {}
}
Singleton.foo()

// After (conceptual)
object Singleton {
    fun foo() {}            // dispatch receiver dropped
}
Singleton.foo()             // emitted as a static call to Singleton.foo
```

### RepeatedAnnotationLowering

When the same `@Repeatable` annotation is applied multiple times to one element, wraps the
repeats into a synthetic `Container` annotation (generating the container nested class if the
user did not declare one).

```kotlin
// Before
@Repeatable annotation class Tag(val v: String)

@Tag("a") @Tag("b") @Tag("c") fun f() {}

// After (conceptual)
@Tag.Container(value = [Tag("a"), Tag("b"), Tag("c")]) fun f() {}
```

---

## 2. File phases (`jvmFilePhases`)

### TypeAliasAnnotationMethodsLowering

For each annotated `typealias`, generates a synthetic empty static method named
`<aliasName>$annotations()` that carries the annotations. Lets reflection see the annotations
across modules.

```kotlin
// Before
@Deprecated("…") typealias Name = String

// After (conceptual)
typealias Name = String
@Deprecated("…") /* synthetic */ fun Name${'$'}annotations() {}
```

### PatchLambdaOffsetsLowering

Adjusts `startOffset`/`endOffset` of lambda-derived function references so that debugger
suspension points line up with the call site / variable assignment, rather than the lambda
literal itself.

_Metadata only — only source-offset metadata changes._

### JvmVersionOverloadsLowering

Generates JVM-target-version-aware overload stubs for functions whose signature has changed
across language versions, and strips the version-overload markers from the generated wrappers.

_Metadata only at the source level — adds extra overloads but does not change call sites._

### JvmOverloadsAnnotationLowering

For functions / constructors annotated `@JvmOverloads` with default parameters, generates one
overload per right-trimmed prefix of the parameter list, each delegating to the original.

```kotlin
// Before
@JvmOverloads
fun greet(name: String = "world", excl: Boolean = false) { /* ... */ }

// After (conceptual)
fun greet(name: String, excl: Boolean) { /* original body */ }
fun greet(name: String) = greet(name, false)
fun greet()             = greet("world", false)
```

### MainMethodGenerationLowering

For parameterless `main()` and `suspend fun main(...)`, generates a JVM-standard
`public static void main(String[])` entry point that delegates to the user's function. Suspend
mains are launched via `kotlin.coroutines.jvm.internal.runSuspend`.

```kotlin
// Before
suspend fun main() { /* ... */ }

// After (conceptual)
fun main(args: Array<String>) {
    kotlin.coroutines.jvm.internal.runSuspend { main() }
}
```

### AnnotationLowering

Removes constructors from annotation classes — on the JVM, annotation "instances" are created
by the runtime, so the IR constructor is unused.

```kotlin
// Before
annotation class Tag(val v: String) {
    constructor(): this("default")
}

// After (conceptual)
annotation class Tag(val v: String)
```

### JvmAnnotationImplementationLowering

For runtime-retained annotations, generates a hidden implementation class with `equals` /
`hashCode` / `toString` / `annotationType` matching the JDK's annotation contract. Used when
code instantiates an annotation programmatically.

_Mostly metadata: adds a synthetic implementation class to the IR; only relevant when the
annotation is constructed at runtime._

### PolymorphicSignatureLowering

For methods marked with JDK's `@PolymorphicSignature` (e.g. `MethodHandle.invokeExact`),
generates a fake call-site-specific overload that matches the actual argument and return types
at the call site, so that the call passes Kotlin type-checking.

```kotlin
// Before
val mh: MethodHandle = ...
val s = mh.invokeExact("x") as String

// After (conceptual)
val mh: MethodHandle = ...
val s: String = mh.invokeExact("x")    // resolves to a synthetic (String) -> String overload
```

### VarargLowering

Replaces `vararg` parameters with array parameters and lowers `arrayOf(...)` /
`emptyArray()` call sites accordingly. Spreads (`*xs`) are unfolded into array copies.

```kotlin
// Before
fun sum(vararg xs: Int): Int = xs.sum()
sum(1, 2, 3)

// After (conceptual)
fun sum(xs: IntArray): Int = xs.sum()
sum(intArrayOf(1, 2, 3))
```

### JvmLateinitLowering

Delegates to the common `LateinitLowering` (which inserts `isInitialized` checks and throws on
unset access), and additionally aligns the backing field's visibility with the property's
setter visibility for JVM ABI reasons.

```kotlin
// Before
lateinit var x: String

// After (conceptual)
private var x_field: String? = null
var x: String
    get() = x_field ?: throw UninitializedPropertyAccessException("…")
    set(v) { x_field = v }
```

### JvmInventNamesForLocalClasses

Computes JVM-internal names for every local / anonymous class and stores them in the IR so
later phases (and metadata) can use a stable JVM identifier.

_Metadata only — sets `localClassType` on `IrClass` nodes._

### PrepareCallableReferencesForInlining

Marks function/property references that will be passed to inline functions with the
`INLINE_LAMBDA` origin, and converts inlinable property references into function references so
the inliner can inline them like ordinary lambdas.

_Metadata only — annotates references; no source-level shape change yet._

### DirectInvokeLowering

If a lambda or function reference is `invoke()`'d immediately at the call site, replaces the
indirection with a direct call (or, for lambdas, with the inlined lambda body).

```kotlin
// Before
val n = ({ x: Int -> x + 1 }).invoke(5)

// After (conceptual)
val lambda = { x: Int -> x + 1 }
val n = lambda(5)        // and the inliner can fold it further to `5 + 1`
```

### FunctionReferenceLowering

Materializes plain (non-`@JvmInline`-eligible) function references as anonymous subclasses of
the appropriate `KFunctionN` / `FunctionReferenceImpl`, with bound receivers stored as fields.

```kotlin
// Before
val ref = ::greet

// After (conceptual)
val ref: KFunction0<Unit> = object : FunctionReferenceImpl(0, ...), KFunction0<Unit> {
    override fun invoke() = greet()
}
```

### SuspendLambdaLowering

Compiles suspend lambdas into named subclasses of `SuspendLambda` containing a coroutine state
machine. Captured variables become fields; the lambda body becomes a `invokeSuspend` with
labels and a `label` field driving the state machine.

```kotlin
// Before
val block: suspend (Int) -> Int = { x -> delay(1); x + 1 }

// After (conceptual)
class Block$1(completion: Continuation<Any?>) : SuspendLambda(2, completion), Function2<Int, Continuation<Int>, Any?> {
    var label = 0
    override fun invokeSuspend(result: Any?): Any? { /* state machine */ }
}
val block = Block$1(/*completion*/)
```

### PropertyReferenceDelegationLowering

Optimizes `by ::someProperty` so that the delegate's `getValue`/`setValue` are inlined as
direct property accesses, instead of constructing a `KProperty` and calling through it.

```kotlin
// Before
class C { var x: Int by ::storage }

// After (conceptual)
class C {
    var x: Int
        get() = storage
        set(v) { storage = v }
}
```

### SingletonOrConstantDelegationLowering

When a `by` delegate is a singleton object, a constant, or another stable expression, removes
the delegate field entirely and rewires the accessors directly to the delegate's
`getValue`/`setValue` calls.

```kotlin
// Before
val v: String by SOME_OBJECT

// After (conceptual)
val v: String
    get() = SOME_OBJECT.getValue(/* thisRef */ null, /* property */ ::v)
```

### PropertyReferenceLowering

Materializes property references (`A::x`, `a::x`, local `::p`) as anonymous subclasses of the
appropriate `KMutableProperty[01-N]` / `KProperty[01-N]`, with bound receivers stored in
fields. Sibling of `FunctionReferenceLowering`.

```kotlin
// Before
val ref = String::length

// After (conceptual)
val ref: KProperty1<String, Int> = object : PropertyReference1Impl(...), KProperty1<String, Int> {
    override fun get(receiver: String) = receiver.length
    override fun invoke(receiver: String) = receiver.length
}
```

### ArrayConstructorLowering

Replaces `Array(size) { init }` (and the primitive-array variants) with a sized allocation
followed by an explicit index-driven loop.

```kotlin
// Before
val sq = Array(n) { i -> i * i }

// After (conceptual)
val sq = arrayOfNulls<Int>(n)
for (i in 0 until n) sq[i] = i * i
```

> *Note from `JvmLoweringPhases.kt`*: the next three phases
> (`MoveOrCopyCompanionObjectFieldsLowering`, `JvmPropertiesLowering`,
> `RemapObjectFieldAccesses`) are intended to be merged eventually — visitors behave
> incorrectly between them because backing fields moved out of companion objects are reachable
> by two paths.

### MoveOrCopyCompanionObjectFieldsLowering

Lifts companion-object backing fields up to *static* fields of the enclosing class. `const`
public fields are *copied* (not moved) so their value can be inlined at call sites for
binary-compat reasons.

```kotlin
// Before
class Outer {
    companion object {
        val x = 1
        const val K = 2
    }
}

// After (conceptual)
class Outer {
    /* static */ val x = 1                     // moved
    /* static */ const val K = 2                // copied; companion still has it
    companion object { const val K = 2 }
}
```

### JvmPropertiesLowering

Splits each `IrProperty` into separate field + accessor declarations on the class, inlines
trivial accessor bodies into call sites, and drops accessors that are unused.

```kotlin
// Before
class C { val x: Int = 1 }

// After (conceptual)
class C {
    /* private */ val x_field: Int = 1
    fun getX(): Int = x_field
}
```

### RemapObjectFieldAccesses

After the previous two phases, rewrites field reads/writes that targeted a companion property
so they hit the static field on the outer class instead of going through the now-empty
companion.

_Metadata only — call sites point to the new static field; no shape change._

### AnonymousObjectSuperConstructorLowering

If the super-constructor call of an anonymous object has a non-trivial argument expression,
hoists the expression out into a constructor parameter on a synthetic local class, so the
inliner is not asked to inline through an "uninitialized this" context.

```kotlin
// Before
run {
    object : Base(complex(side = effect)) { /* ... */ }
}

// After (conceptual)
run {
    class Anon(arg: T) : Base(arg) { /* ... */ }
    Anon(complex(side = effect))
}
```

### JvmBuiltInsLowering

Replaces a small set of stdlib built-in calls with JVM-friendly forms — for example, unsigned
integer comparisons / arithmetic are rewritten to use signed primitives plus the appropriate
helper, and `arrayOf` / `emptyArray` are folded.

```kotlin
// Before
val cmp = a.toUInt().compareTo(b.toUInt())

// After (conceptual)
val cmp = unsignedCompare(a.toInt(), b.toInt())   // exact runtime helper depends on type
```

### RangeContainsLowering

Optimizes `x in a..b` (and `!in`) for closed ranges of primitive / comparable types into
direct boundary comparisons, avoiding allocation of a range object.

```kotlin
// Before
val ok = x in 1..10

// After (conceptual)
val ok = 1 <= x && x <= 10
```

### ForLoopsLowering

Replaces `for (i in progression-or-array)` with a primitive-induction-variable `while` loop,
avoiding the iterator allocation.

```kotlin
// Before
for (i in 1..10) println(i)

// After (conceptual)
var i = 1
val last = 10
if (i <= last) do {
    println(i)
    i++
} while (i <= last)
```

### CollectionStubMethodLowering

For Kotlin classes that implement read-only collection interfaces (`List`, `Set`, ...), adds
synthetic `add` / `remove` / `clear` / etc. methods that throw `UnsupportedOperationException`,
because on the JVM those interfaces are merged with the mutable JDK ones.

```kotlin
// Before
class IntList : List<Int> { /* ... */ }

// After (conceptual)
class IntList : List<Int> {
    /* ... */
    fun add(e: Int): Boolean = throw UnsupportedOperationException("Operation is not supported for read-only collection")
    fun clear()              = throw UnsupportedOperationException(/* ... */)
}
```

### JvmSingleAbstractMethodLowering

Materializes coercion of a lambda / function reference to a `fun interface` (Kotlin SAM) into
an anonymous class implementing that interface.

```kotlin
// Before
fun interface Listener { fun onEvent(x: Int) }
val l: Listener = { x -> println(x) }

// After (conceptual)
val l: Listener = object : Listener {
    override fun onEvent(x: Int) = println(x)
}
```

### JvmInlineMultiFieldValueClassLowering

Implements multi-field value classes (an experimental extension of `@JvmInline`): replaces
parameters and locals of MFVC types with their flattened underlying components, and elides
boxing whenever possible.

_Mostly metadata at the source level — function signatures change, fields get split, but the
high-level program shape is preserved._

### JvmInlineClassLowering

For single-field `@JvmInline value class`es, unboxes the underlying type in function
signatures, generates `box-impl`/`unbox-impl` helpers, and renames the original entry points
to `*-impl` so they do not clash with the boxed bridges.

```kotlin
// Before
@JvmInline value class UserId(val raw: Long)
fun lookup(id: UserId): String = "..."

// After (conceptual)
fun lookup-impl(id: Long): String = "..."         // unboxed
fun lookup(id: UserId): String = lookup-impl(id.raw)  // boxed bridge
```

### JvmTailrecLowering

Rewrites `tailrec` recursive calls into a `while (true)` loop that reassigns parameters and
loops back to the start of the function.

```kotlin
// Before
tailrec fun fact(n: Int, acc: Long = 1L): Long =
    if (n <= 1) acc else fact(n - 1, acc * n)

// After (conceptual)
fun fact(n: Int, acc: Long = 1L): Long {
    var n_ = n; var acc_ = acc
    while (true) {
        if (n_ <= 1) return acc_
        val newAcc = acc_ * n_
        n_ = n_ - 1
        acc_ = newAcc
    }
}
```

### MappedEnumWhenLowering

Rewrites `when` on an enum subject to look up the branch via an int array indexed by
`ordinal()`, populated in a generated `WhenMappings` class. This way, adding a new entry to
the enum does not invalidate the call site's bytecode.

```kotlin
// Before
when (e) { E.X -> 1; E.Y -> 2; else -> 0 }

// After (conceptual)
val map = WhenMappings.${'$'}EnumSwitchMapping${'$'}0   // int[] populated once
when (map[e.ordinal]) { 1 -> 1; 2 -> 2; else -> 0 }
```

### AssertionLowering

Lowers `assert(cond)` (or `assert(cond) { msg }`) according to compiler mode: always-on,
always-off, or runtime-checked via a class-level `${'$'}assertionsDisabled` flag (the standard
JVM idiom).

```kotlin
// Before
assert(x > 0) { "x was $x" }

// After (conceptual, JVM-runtime mode)
if (!${'$'}assertionsDisabled && !(x > 0)) throw AssertionError("x was $x")
```

### JvmReturnableBlockLowering

Replaces `IrReturnableBlock` (a block with a non-local return target, used by inlined bodies)
with a `do { ... } while (false)` plus `break` to its label.

```kotlin
// Before — inlined block whose body wants to return a value
returnable@ {
    if (cond) return@returnable a
    b
}

// After (conceptual)
var result: T
loop@ do {
    if (cond) { result = a; break@loop }
    result = b
} while (false)
result
```

### SingletonReferencesLowering

Optimizes references to enum entries and singleton `object`s: inside the entry's own
constructor, replaces the reference with `this`; everywhere else, replaces it with a direct
field load.

```kotlin
// Before
enum class E { A;
    val self = E.A          // forward self-reference inside A's ctor
}

// After (conceptual)
enum class E { A;
    val self = this         // resolved to `this` inside A's ctor
}
```

### JvmSharedVariablesLowering

For local `var`s captured by lambdas and reassigned, wraps them in mutable `Ref` objects so
the captured copy and the outer variable stay in sync.

```kotlin
// Before
var n = 0
listOf(1, 2, 3).forEach { n += it }

// After (conceptual)
val n = Ref.IntRef().also { it.element = 0 }
listOf(1, 2, 3).forEach { n.element += it }
```

### JvmInventNamesForLocalFunctions

Generates unique, JVM-identifier-safe names for local functions discovered during lowering.
Sibling of `JvmInventNamesForLocalClasses`, but for functions.

_Metadata only — only `IrFunction.name` changes._

### JvmLocalDeclarationsLowering

Lifts local functions and local classes out to top-level (or to enclosing class scope),
turning captured outer values into constructor parameters / fields.

```kotlin
// Before
fun outer() {
    val captured = 1
    fun inner() = captured + 1
    inner()
}

// After (conceptual)
class Outer${'$'}inner${'$'}1(val captured: Int) {
    fun invoke(): Int = captured + 1
}
fun outer() {
    val captured = 1
    Outer${'$'}inner${'$'}1(captured).invoke()
}
```

### JvmLocalDeclarationPopupLowering

If a local class appears inside a property initializer or `init {}` block, hoists it out to
the enclosing class, so that `JvmInitializersLowering` does not duplicate it across multiple
constructors.

```kotlin
// Before
class Outer {
    val x = object : Base() { /* ... */ }
}

// After (conceptual)
class Outer {
    val x = Outer${'$'}1()
}
class Outer${'$'}1 : Base() { /* ... */ }
```

### StaticCallableReferenceLowering

For function-reference subclasses synthesized by `FunctionReferenceLowering` / others that have
no captured state, replaces every constructor call with a load of a singleton `INSTANCE`
field, and adds the field + initializer.

```kotlin
// Before
val ref = ::someTopLevel       // produces `FooReference()`

// After (conceptual)
val ref = FooReference.INSTANCE
```

### JvmDefaultConstructorLowering

If a class has a primary constructor whose every parameter has a default value, generates a
parameterless secondary constructor that delegates to it. Helps Java reflection libraries
(Jackson, JPA, ...) that expect a no-arg constructor.

```kotlin
// Before
class Foo(val x: Int = 1, val y: String = "")

// After (conceptual)
class Foo(val x: Int = 1, val y: String = "") {
    constructor() : this(1, "")
}
```

### FlattenStringConcatenationLowering

Folds chains of `String.plus` calls and nested `IrStringConcatenation` nodes into a single
`IrStringConcatenation`, so the JVM concatenation lowering can later emit one `StringBuilder`
chain instead of many.

```kotlin
// Before
val s = "a" + (1 + ("b" + 2.0))

// After (conceptual)
val s = "$a${'$'}{1}${'$'}b${'$'}{2.0}"        // a single concatenation node with 4 operands
```

### JvmStringConcatenationLowering

Lowers each `IrStringConcatenation` into the JVM idiom: `StringBuilder().append(a).append(b)…
.toString()`, normalizing primitive widths and inline-class operands along the way.

```kotlin
// Before  (after FlattenStringConcatenation)
val s = "a${'$'}{n}b${'$'}{d}"

// After (conceptual)
val s = StringBuilder().append("a").append(n).append("b").append(d).toString()
```

### JvmDefaultArgumentStubGenerator

For each function with default parameter values, generates a `$default` stub: an extra
function taking all parameters plus a bitmask indicating which were supplied; the stub fills
in defaults and delegates to the original.

```kotlin
// Before
fun greet(name: String = "world", excl: Boolean = false) = ...

// After (conceptual)
fun greet(name: String, excl: Boolean) = ...                    // original
fun greet${'$'}default(name: String?, excl: Boolean, mask: Int, marker: Any?): Unit {
    val n = if (mask and 1 != 0) "world" else name!!
    val e = if (mask and 2 != 0) false   else excl
    return greet(n, e)
}
```

### JvmDefaultParameterInjector

Rewrites *call sites* that omit some arguments of a function with defaults, redirecting them
to the `$default` stub generated by the previous phase, with an appropriate bitmask.

```kotlin
// Before
greet(excl = true)

// After (conceptual)
greet${'$'}default(/*name*/null, /*excl*/true, /*mask*/ 0b01, /*marker*/null)
```

### JvmDefaultParameterCleaner

After call sites have been rewired, strips the now-redundant default-value expressions from
the original function's parameter list (the defaults live only in the stub).

_Metadata only — `IrValueParameter.defaultValue` is cleared._

### InterfaceLowering

The big interface-lowering: moves default method bodies into a synthetic `DefaultImpls`
nested class (or generates `JvmDefault` JVM 8+ defaults, depending on mode), generates static
delegates, and removes private interface members.

```kotlin
// Before
interface I {
    fun foo() = 1
    fun bar()
}

// After (conceptual, default-impls mode)
interface I {
    fun foo(): Int            // abstract
    fun bar()
    class DefaultImpls {
        @JvmStatic fun foo(${'$'}this: I) = 1
    }
}
```

### InheritedDefaultMethodsOnClassesLowering

For a concrete class that *inherits* (rather than overrides) an interface default method,
generates a bridge method on the class that delegates to the interface's `DefaultImpls`. Also
handles synthesizing `clone()` for `Cloneable` implementors.

```kotlin
// Before
interface I { fun foo() = 1 }
class C : I

// After (conceptual)
interface I { fun foo(): Int /* abstract after InterfaceLowering */ }
class C : I {
    override fun foo() = I.DefaultImpls.foo(this)
}
```

### GenerateJvmDefaultCompatibilityBridges

In `-jvm-default=enable` / `-jvm-default=no-compatibility` modes, generates compatibility
bridges that delegate from a fake-override into the appropriate super-interface default, so
that older clients linking against the old shape continue to work.

_Metadata only — the new bridge methods are inserted; user code is unaffected._

### InterfaceSuperCallsLowering

Rewrites `super<I>.foo()` calls into static calls to `I.DefaultImpls.foo(this)`, since the
default body now lives there.

```kotlin
// Before
class C : I { override fun foo() = super<I>.foo() }

// After (conceptual)
class C : I { override fun foo() = I.DefaultImpls.foo(this) }
```

### InterfaceDefaultCallsLowering

When a call resolves to an interface function whose body lives in `DefaultImpls`, redirects
the call site to `DefaultImpls.foo(receiver, ...)`. Skips functions that are compiled to real
JVM `default` methods.

_Metadata only — call targets are rewired; argument shape is unchanged except for the moved
dispatch receiver._

### InterfaceObjectCallsLowering

Resolves calls to `Any`/`Object` methods (`toString`, `hashCode`, `equals`) made through an
interface receiver to a real virtual dispatch, rather than going through fake-override
machinery.

_Metadata only — adjusts call resolution / super qualifier._

### TailCallOptimizationLowering

For `suspend` functions whose return is in tail position, inserts an explicit `return`
statement so the existing JVM bytecode tail-call optimizer recognizes the call.

```kotlin
// Before
suspend fun fwd(): T = delegate()

// After (conceptual — the IR explicitly returns the result)
suspend fun fwd(): T { return delegate() }
```

### AddContinuationLowering

Transforms `suspend` functions to take an extra `Continuation` parameter, generates the
coroutine state-machine class, and lowers the body into a switch over a `label` field.

```kotlin
// Before
suspend fun load(id: Int): String { val r = fetch(id); return r.parse() }

// After (conceptual)
fun load(id: Int, ${'$'}cont: Continuation<String>): Any? {
    class StateMachine : ContinuationImpl(${'$'}cont) { /* label, locals, resumeWith */ }
    val sm = (${'$'}cont as? StateMachine) ?: StateMachine()
    /* switch on sm.label, calls fetch(id) suspending, then parse, then return */
}
```

### JvmInnerClassesLowering

Adds the synthetic `this${'$'}0` field that holds the outer instance, plus the corresponding
constructor parameter, on every `inner class`.

```kotlin
// Before
class Outer { inner class Inner }

// After (conceptual)
class Outer {
    inner class Inner(/* synth */ val this${'$'}0: Outer)
}
```

### JvmInnerClassesMemberBodyLowering

Inside `inner class` bodies, rewrites references to outer-instance members so they go through
the new `this${'$'}0` field added by the previous phase.

```kotlin
// Before — inside Outer.Inner
fun show() = field          // refers to Outer.field

// After (conceptual)
fun show() = this${'$'}0.field
```

### JvmInnerClassConstructorCallsLowering

At every call site that constructs an `inner class`, threads the outer instance through as the
first constructor argument.

```kotlin
// Before
val outer = Outer()
val inner = outer.Inner()

// After (conceptual)
val outer = Outer()
val inner = Outer.Inner(outer)
```

### EnumClassLowering

Materializes `enum class` declarations: turns each entry into a `static final` field,
generates the `${'$'}VALUES` array and `${'$'}ENTRIES` field, and synthesizes
`values()` / `valueOf()` / `entries`. Adds `${'$'}enum${'$'}name` and `${'$'}enum${'$'}ordinal`
parameters to enum constructors.

```kotlin
// Before
enum class E { A, B }

// After (conceptual)
class E private constructor(name: String, ordinal: Int) : Enum<E>(name, ordinal) {
    companion object {
        val A = E("A", 0)
        val B = E("B", 1)
        val ${'$'}VALUES: Array<E> = arrayOf(A, B)
        fun values()  = ${'$'}VALUES.clone()
        fun valueOf(s: String) = /* lookup */
    }
}
```

### EnumExternalEntriesLowering

For `.entries` accesses on enums that are *not* compiled by this run (Java enums,
already-compiled Kotlin enums in dependencies), generates a per-file `EntriesMappings` class
containing static `EnumEntries` fields and rewrites the access to load that field.

```kotlin
// Before
val list = JavaEnum.entries

// After (conceptual)
val list = FileKt${'$'}EntriesMappings.entries${'$'}0   // EnumEntries<JavaEnum> built once
```

### ObjectClassLowering

Materializes `object` declarations as classes with a `static final INSTANCE` field initialized
in `<clinit>`. Adds private alternate fields when needed for visibility-sensitive callers.

```kotlin
// Before
object Config { val timeout = 5 }

// After (conceptual)
class Config private constructor() {
    val timeout = 5
    companion object { /* static */ val INSTANCE = Config() }
}
```

### IndyLambdaMetafactoryLowering

When the compile target supports it, replaces lambda / function-reference materialization with
`invokedynamic` + `LambdaMetafactory` calls, instead of generating a full anonymous class.

_Metadata only at the source level — no Kotlin-shape change; the bootstrap is emitted at
codegen time._

### StaticInitializersLowering

Collects static field initializers and static `init {}` blocks into a single synthetic
`<clinit>` function, ordered so generated property refs / enum fields / object instances run
before user code.

```kotlin
// Before
class C {
    companion object {
        /* static */ val a = compute()
        init { register() }
    }
}

// After (conceptual)
class C {
    companion object {
        /* static */ val a: Int
        ${'$'}clinit { a = compute(); register() }
    }
}
```

### UniqueLoopLabelsLowering

Assigns a unique label string to every `IrLoop` (combining the enclosing scope name plus a
counter), so that non-local `break`/`continue` bytecode can be generated unambiguously.

_Metadata only — only `IrLoop.label` changes._

### JvmInitializersLowering

Folds field initializers and `init {}` blocks into each constructor body, in declaration
order, so that codegen does not need to reason about `init` blocks separately.

```kotlin
// Before
class C(val n: Int) {
    val x = compute(n)
    init { register(this) }
}

// After (conceptual)
class C(val n: Int) {
    val x: Int
    constructor(n: Int) {
        this.x = compute(n)
        register(this)
    }
}
```

### JvmInitializersCleanupLowering

Strips the now-redundant non-static `init {}` blocks and non-`const` field initializers — they
have already been merged into constructors. Static `const`-field initializers stay because
the JVM emits a `ConstantValue` attribute from them.

_Metadata only — removes already-relocated nodes._

### FunctionNVarargBridgeLowering

For function types with arity ≥ 23 (the JVM "BigArity" cutoff), replaces the
`Function23..FunctionN` supertype with `FunctionN` and adds a `vararg`-shaped bridge that
unpacks an `Array<Any?>` into the individual `invoke` parameters.

```kotlin
// Before
class Big : Function23<...> { override fun invoke(p1, ..., p23) = ... }

// After (conceptual)
class Big : FunctionN<Any?> {
    override fun invoke(vararg args: Any?): Any? {
        require(args.size == 23)
        return invoke23(args[0] as ..., /* ... */, args[22] as ...)
    }
    fun invoke23(p1, ..., p23) = /* original body */
}
```

### JvmStaticInCompanionLowering

For `@JvmStatic` members of *companion* objects, generates a static proxy on the enclosing
class that delegates to the companion's instance method.

```kotlin
// Before
class Foo {
    companion object {
        @JvmStatic fun ping() = "pong"
    }
}

// After (conceptual)
class Foo {
    companion object { fun ping() = "pong" }
    /* static */ fun ping() = Foo.Companion.ping()
}
```

### StaticDefaultFunctionLowering

Converts `$default` stubs (generated by `JvmDefaultArgumentStubGenerator`) from instance to
static methods, taking the receiver as a regular parameter. This is what the JVM expects for
default-argument helpers.

```kotlin
// Before
class C { fun foo${'$'}default(this${'$'}0: C, mask: Int, /* args */) = ... }

// After (conceptual)
class C { /* static */ fun foo${'$'}default(this${'$'}0: C, mask: Int, /* args */) = ... }
```

### BridgeLowering

Generates JVM bridge methods to fix virtual dispatch after generic erasure (e.g. an override
that returns `String` needs an `Object`-returning bridge), and to adapt Kotlin collections to
their JDK counterparts.

```kotlin
// Before
class C : Comparable<C> { override fun compareTo(other: C) = 0 }

// After (conceptual)
class C : Comparable<C> {
    override fun compareTo(other: C) = 0
    /* synth */ fun compareTo(other: Any?): Int = compareTo(other as C)   // bridge
}
```

### SyntheticAccessorLowering

When a member is referenced from a context that JVM rules forbid (e.g. private member of an
outer class accessed from an inner class, or protected member from another package), generates
a package-private synthetic `access$N` accessor and rewires the call.

```kotlin
// Before
class Outer {
    private fun secret() = 42
    inner class Inner { fun use() = secret() }     // illegal on JVM
}

// After (conceptual)
class Outer {
    private fun secret() = 42
    /* synth */ fun access${'$'}secret() = secret()
    inner class Inner { fun use() = this${'$'}0.access${'$'}secret() }
}
```

### JvmArgumentNullabilityAssertionsLowering

Inserts or removes `kotlin.jvm.internal.Intrinsics.checkNotNull*` calls for non-null parameter
/ receiver assertions, depending on `-Xno-call-assertions` / `-Xno-receiver-assertions` /
unified-null-check settings.

```kotlin
// Before  (function with platform-type non-null param)
fun greet(name: String) = "hi $name"

// After (conceptual)
fun greet(name: String) {
    kotlin.jvm.internal.Intrinsics.checkNotNullParameter(name, "name")
    return "hi $name"
}
```

### ToArrayLowering

For Kotlin classes that implement `Collection`, generates the two `toArray()` overloads
required by the Java `Collection` interface (no-arg `toArray(): Array<Any?>` and generic
`toArray(T[]): T[]`), delegating to a stdlib helper.

```kotlin
// Before
class IntBag : Collection<Int> { /* ... */ }

// After (conceptual)
class IntBag : Collection<Int> {
    /* ... */
    override fun toArray(): Array<Any?>           = CollectionToArray.toArray(this)
    override fun <T> toArray(a: Array<T?>): Array<T?> = CollectionToArray.toArray(this, a)
}
```

### JvmSafeCallChainFoldingLowering

Folds chains like `a?.b?.c` and `a ?: b` into a flatter form (single null check + `when`)
that the bytecode optimizer handles better than nested conditionals.

```kotlin
// Before
val x = a?.b()?.c() ?: fallback

// After (conceptual)
val ta = a
val x = if (ta == null) fallback else {
    val tb = ta.b()
    if (tb == null) fallback else (tb.c() ?: fallback)
}
```

### JvmOptimizationLowering

A grab-bag of peephole IR optimizations: double-negation elimination, constant-comparison
folding, inlining of trivial final-getter/setter accessors, etc.

```kotlin
// Before
val ok = !!cond
val same = (null == null)

// After (conceptual)
val ok = cond
val same = true
```

### AdditionalClassAnnotationLowering

For Kotlin annotation classes, synthesizes the matching JDK meta-annotations
(`@java.lang.annotation.Retention`, `@Target`, `@Documented`, `@Repeatable`) so the resulting
class file is a valid JDK annotation type.

_Metadata only — adds JVM-side annotations on the class._

### RecordEnclosingMethodsLowering

Records `EnclosingMethod` metadata for lambdas and local classes that were created inside
inline functions or by `LambdaMetafactory`, so reflection / debuggers can recover the source
context.

_Metadata only — sets `enclosingMethodOverride` on `IrFunction`._

### TypeOperatorLowering

Lowers `as` / `as?` / `is` operators into JVM-shaped checks: `instanceof` for non-nullable
checks, null-aware combinations for nullable types, throws for failed `as`. After this phase,
only `IMPLICIT_CAST` / reified `SAFE_CAST` / non-nullable `INSTANCE_OF`/`CAST` survive.

```kotlin
// Before
val ok = x is String
val s  = x as String

// After (conceptual)
val ok = x is /* erased */ String
val s  = (x as String?) ?: throw TypeCastException("…")
```

### ReplaceKFunctionInvokeWithFunctionInvoke

`KFunction<R>` has no arity in its type, but the runtime classes do. Replaces
`(kf as KFunction).invoke(args)` calls with an explicit cast to the matching `FunctionN` and a
call to *its* `invoke`.

```kotlin
// Before
val kf: KFunction2<Int, Int, Int> = ::plus
val r = kf.invoke(1, 2)

// After (conceptual)
val kf: KFunction2<Int, Int, Int> = ::plus
val r = (kf as Function2<Int, Int, Int>).invoke(1, 2)
```

### JvmKotlinNothingValueExceptionLowering

When a non-suspend call returns `Nothing`, inserts a JVM-visible "unreachable" marker after it
so the verifier knows control cannot continue.

```kotlin
// Before
fun crash(): Nothing = error("boom")
val x = crash()

// After (conceptual)
fun crash(): Nothing = error("boom")
val x = run { crash(); throw KotlinNothingValueException() }
```

### MakePropertyDelegateMethodsStaticLowering

Turns the synthetic `$delegate` accessor methods (generated for optimized property references)
from instance into static methods. Runs after `JvmLocalDeclarationsLowering` so any captured
state is already on the class.

_Metadata only — adjusts dispatch receivers; no source-level shape change._

### ReplaceNumberToCharCallSitesLowering

Replaces `someNumber.toChar()` (deprecated on `kotlin.Number` since 1.5) with
`someNumber.toInt().toChar()`. Only applies when `toChar` resolves to the `Number` declaration,
not to a user override.

```kotlin
// Before
val c: Char = (n as Number).toChar()

// After (conceptual)
val c: Char = (n as Number).toInt().toChar()
```

### RenameFieldsLowering

Resolves JVM field-name clashes (most often introduced by
`MoveOrCopyCompanionObjectFieldsLowering` copying a `const` from the companion to the outer
class). Lower-priority duplicates are renamed to `name${'$'}1`, `name${'$'}2`, …

```kotlin
// Before — both fields literally named `K` after companion-field lifting
class C { /* static */ const val K = 1; companion object { const val K = 1 } }

// After (conceptual)
class C { /* static */ const val K = 1; companion object { /* static */ const val K${'$'}1 = 1 } }
```

### FakeLocalVariablesForBytecodeInlinerLowering

Inserts synthetic local variables with magic name prefixes (`${'$'}i$f$`, `${'$'}i$a$`) at the
boundaries of inline-function and inline-lambda regions. The JVM bytecode inliner looks for
those names to find the regions it must inline.

_Metadata only — adds local-variable declarations consumed by the bytecode inliner._

### SpecialAccessLowering

Active only for debugger expression evaluation: rewrites accesses to private/protected members
in the evaluated expression to use `java.lang.reflect.{Method,Field}.{invoke,get,set}` so the
expression can run from outside the host class.

_Metadata only — only present in evaluator mode; ordinary compilation is unaffected._

### TypeSwitchLowering

When targeting JVM 21+ with `-Xindy-allowed-instructions=..`, optimizes type-checking `when`
expressions into a single `invokedynamic` `typeSwitch`, replacing the cascade of `instanceof`
checks with one switch.

```kotlin
// Before
when (x) { is A -> 1; is B -> 2; else -> 0 }

// After (conceptual — JVM 21 indy)
when (typeSwitchIndy(x, A::class, B::class)) {  // -1 == else
    0    -> 1
    1    -> 2
    else -> 0
}
```

---

## 3. Module phases — post-file (`jvmModulePhases2`)

### GenerateMultifileFacades

For files marked `@file:JvmMultifileClass`, generates a single facade `IrFile` that holds the
public methods and properties of all parts. In `inheritMultifileParts` mode, parts are made
to extend each other so the facade can inherit instead of re-emitting bridges.

_Metadata only at the source level — produces a new synthetic file containing the facade
class and rewires public call sites._

### ResolveInlineCalls

Rewrites calls to `inline fun` so that they target the actual implementation rather than a
fake-override / facade indirection. Necessary so that the JVM inliner can actually inline the
body across module boundaries.

_Metadata only — only call targets change; no shape change._

### JvmIrValidationAfterLoweringPhase

Runs final JVM-specific IR validation: confirms there are no `IrProperty`,
`IrAnonymousInitializer` or non-`IrClass` top-level declarations left, and that all the
post-conditions of the JVM lowering pipeline hold. Fails compilation if violated.

_Metadata only — pure validation pass._
