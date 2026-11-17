// ISSUE: KT-89978
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// The test checks an optimization which is implemented only for JS and Native backends:
// - JS output is verified with `CHECK_NOT_CALLED_IN_SCOPE` and `CHECK_NEW_COUNT` directives
// - Native output is verified with FileCheck directives
// Wasm does not have it (KT-90128), which is pinned with `WASM_COUNT_INSTRUCTION_IN_FUNCTION` directives: the comparisons allocate there.

value class Single(val x: Int)
value class Outer(val single: Single)
value class OverUShort(val x: UShort)
value class OuterOverUShort(val single: OverUShort)

// CHECK-LABEL: define ptr @"kfun:#test(){}kotlin.String"
// CHECK-NOT: <Single-box>
// CHECK-NOT: <Outer-box>
// CHECK-NOT: Single#equals
// CHECK-NOT: Outer#equals
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=equals
// CHECK_NEW_COUNT: function=test count=0
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=test count=8
fun test(): String {
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
// CHECK-NOT: OverUShort#equals
// CHECK-NOT: OuterOverUShort#equals
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testOverUShort function=equals
// CHECK_NEW_COUNT: function=testOverUShort count=0
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverUShort count=8
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

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    val result = test()
    if (result != "OK") return result
    return testOverUShort()
}
