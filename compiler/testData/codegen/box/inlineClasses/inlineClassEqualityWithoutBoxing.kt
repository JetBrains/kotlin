// ISSUE: KT-91448, KT-86967
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// The test checks an optimization which is implemented for JS, Native and Wasm backends:
// - JS output is verified with `CHECK_NOT_CALLED_IN_SCOPE` and `CHECK_NEW_COUNT` directives; the latter is limited to `JS_IR`,
//   as constructors are called through factory functions in ES6 mode
// - Native output is verified with FileCheck directives
// - Wasm output is verified with `WASM_COUNT_INSTRUCTION_IN_FUNCTION` directives
// It also checks binary compatibility with own stdlib, the one built by the same compiler: `==` on `UShort` deserialized from
// its klib, and on inline classes wrapping it, must be neither boxed nor lowered to an `equals` call.
// Compatibility with stdlib built by an older compiler (KT-90675) is checked by `valueClassEqualsDoesNotCallEquals.kt`
// and `valueClassEqualsDoesNotAllocate.kt`.

inline value class Single(val x: Int)
inline value class Outer(val single: Single)
inline value class OverUShort(val x: UShort)
inline value class OuterOverUShort(val single: OverUShort)

// CHECK-LABEL: define ptr @"kfun:#testOverInt(){}kotlin.String"
// CHECK-NOT: <Single-box>
// CHECK-NOT: <Outer-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testOverInt function=equals
// CHECK_NEW_COUNT: function=testOverInt count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverInt count=0
fun testOverInt(): String {
    val a1 = Single(1)
    val b1 = Single(1)
    val a2 = Single(2)

    if (!(a1 == b1)) return "Fail 1"
    if (a1 == a2) return "Fail 2"
    if (!(Outer(a1) == Outer(b1))) return "Fail 3"
    if (Outer(a1) == Outer(a2)) return "Fail 4"
    return "OK"
}

// CHECK-LABEL: define ptr @"kfun:#testOverUShort(){}kotlin.String"
// CHECK-NOT: <UShort-box>
// CHECK-NOT: <OverUShort-box>
// CHECK-NOT: <OuterOverUShort-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testOverUShort function=equals
// CHECK_NEW_COUNT: function=testOverUShort count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverUShort count=0
fun testOverUShort(): String {
    val a1 = OverUShort(1u)
    val b1 = OverUShort(1u)
    val a2 = OverUShort(2u)

    if (!(a1 == b1)) return "Fail 5"
    if (a1 == a2) return "Fail 6"
    if (!(OuterOverUShort(a1) == OuterOverUShort(b1))) return "Fail 7"
    if (OuterOverUShort(a1) == OuterOverUShort(a2)) return "Fail 8"
    return "OK"
}

// `UShort` comes from the stdlib klib, so its `==` must be recognized as the one of an inline class after deserialization.
// CHECK-LABEL: define {{.*}}i1 @"kfun:#equalsUShort(kotlin.UShort;kotlin.UShort){}kotlin.Boolean"
// CHECK-NOT: -box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=equalsUShort function=equals
// CHECK_NEW_COUNT: function=equalsUShort count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=equalsUShort count=0
fun equalsUShort(a: UShort, b: UShort): Boolean = a == b

// CHECK-LABEL: define {{.*}}i16 @"kfun:#wrapAndUnwrap(kotlin.UShort){}kotlin.UShort"
// CHECK-NOT: -box>
// CHECK-NOT: -unbox>
// CHECK-LABEL: epilogue:
// CHECK_NEW_COUNT: function=wrapAndUnwrap count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=wrapAndUnwrap count=0
fun wrapAndUnwrap(a: UShort): UShort = OverUShort(a).x

// Stdlib inline functions (e.g. `UShort.plus`) are deliberately not used here: their inlined bodies leave an `<UInt-unbox>` call
// in an unreachable block at this stage, which is irrelevant to this test.
// CHECK-LABEL: define {{.*}}i16 @"kfun:#select(kotlin.Boolean;OverUShort;OverUShort){}OverUShort"
// CHECK-NOT: -box>
// CHECK-NOT: -unbox>
// CHECK-LABEL: epilogue:
// CHECK_NEW_COUNT: function=select count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=select count=0
fun select(first: Boolean, a: OverUShort, b: OverUShort): OverUShort {
    val result = if (first) a else OverUShort(b.x)
    return result
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    val result = testOverInt()
    if (result != "OK") return result
    val resultOverUShort = testOverUShort()
    if (resultOverUShort != "OK") return resultOverUShort

    if (!equalsUShort(1u, 1u)) return "Fail 9"
    if (equalsUShort(1u, 2u)) return "Fail 10"
    if (wrapAndUnwrap(42u) != 42u.toUShort()) return "Fail 11"
    if (select(true, OverUShort(1u), OverUShort(2u)) != OverUShort(1u)) return "Fail 12"
    if (select(false, OverUShort(1u), OverUShort(2u)) != OverUShort(2u)) return "Fail 13"
    return "OK"
}
