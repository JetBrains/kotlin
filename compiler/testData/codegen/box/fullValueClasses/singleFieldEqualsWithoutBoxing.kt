// ISSUE: KT-89978
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// The test checks an optimization which is implemented only for JS and Native backends:
// - JS output is verified with `CHECK_NOT_CALLED_IN_SCOPE` and `CHECK_NEW_COUNT` directives; the latter is limited to `JS_IR`,
//   as constructors are called through factory functions in ES6 mode
// - Native output is verified with FileCheck directives
// Wasm unwraps only inline classes when comparing (KT-86967), so for full value classes the comparisons allocate there,
// which is pinned with `WASM_COUNT_INSTRUCTION_IN_FUNCTION` directives.

value class Single(val x: Int)
value class Outer(val single: Single)
value class SingleOverUShort(val x: UShort)
value class OuterOverUShort(val single: SingleOverUShort)
inline value class InlineOne(val x: Int)
inline value class InlineOverFull(val single: Single)
value class FullOverInline(val single: InlineOne)

// CHECK-LABEL: define ptr @"kfun:#testOverInt(){}kotlin.String"
// CHECK-NOT: <Single-box>
// CHECK-NOT: <Outer-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testOverInt function=equals
// CHECK_NEW_COUNT: function=testOverInt count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverInt count=8
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
// CHECK-NOT: <SingleOverUShort-box>
// CHECK-NOT: <OuterOverUShort-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testOverUShort function=equals
// CHECK_NEW_COUNT: function=testOverUShort count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverUShort count=8
fun testOverUShort(): String {
    val a1 = SingleOverUShort(1u)
    val b1 = SingleOverUShort(1u)
    val a2 = SingleOverUShort(2u)

    if (!(a1 == b1)) return "Fail 5"
    if (a1 == a2) return "Fail 6"
    if (!(OuterOverUShort(a1) == OuterOverUShort(b1))) return "Fail 7"
    if (OuterOverUShort(a1) == OuterOverUShort(a2)) return "Fail 8"
    return "OK"
}

// CHECK-LABEL: define ptr @"kfun:#testInlineOverFull(){}kotlin.String"
// CHECK-NOT: <Single-box>
// CHECK-NOT: <InlineOverFull-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineOverFull function=equals
// CHECK_NEW_COUNT: function=testInlineOverFull count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testInlineOverFull count=4
fun testInlineOverFull(): String {
    if (!(InlineOverFull(Single(1)) == InlineOverFull(Single(1)))) return "Fail 9"
    if (InlineOverFull(Single(1)) == InlineOverFull(Single(2))) return "Fail 10"
    return "OK"
}

// CHECK-LABEL: define ptr @"kfun:#testFullOverInline(){}kotlin.String"
// CHECK-NOT: <InlineOne-box>
// CHECK-NOT: <FullOverInline-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=testFullOverInline function=equals
// CHECK_NEW_COUNT: function=testFullOverInline count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testFullOverInline count=4
fun testFullOverInline(): String {
    val a1 = InlineOne(1)
    val b1 = InlineOne(1)
    val a2 = InlineOne(2)

    if (!(FullOverInline(a1) == FullOverInline(b1))) return "Fail 11"
    if (FullOverInline(a1) == FullOverInline(a2)) return "Fail 12"
    return "OK"
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    val result = testOverInt()
    if (result != "OK") return result
    val resultOverUShort = testOverUShort()
    if (resultOverUShort != "OK") return resultOverUShort
    val resultInlineOverFull = testInlineOverFull()
    if (resultInlineOverFull != "OK") return resultInlineOverFull
    return testFullOverInline()
}
