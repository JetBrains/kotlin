// ISSUE: KT-89978, KT-91357
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// The test checks that single-field full value classes are not treated as inline classes: their instances are allocated,
// and they are compared with the `equals` function instead of the comparison of the underlying values.
// - JS output is verified with `CHECK_CALLED_IN_SCOPE` and `CHECK_NEW_COUNT` directives; the latter is limited to `JS_IR`,
//   as constructors are called through factory functions in ES6 mode
// - Native output is verified with FileCheck directives: every constructor call and comparison in the order of the source code.
//   The callee of a comparison is `Any#equals` or the devirtualized `equals` of the class, depending on the optimization mode,
//   so only the common part of their names is checked
// - Wasm output is verified with `WASM_CHECK_CALLED_IN_FUNCTION` and `WASM_COUNT_INSTRUCTION_IN_FUNCTION` directives:
//   the constructors and `equals` of the classes are called, and their instances are not allocated in place, as it would be
//   for boxing of an inline class

value class Single(val x: Int)
value class Outer(val single: Single)
value class SingleOverUShort(val x: UShort)
value class OuterOverUShort(val single: SingleOverUShort)
inline value class InlineOne(val x: Int)
inline value class InlineOverFull(val single: Single)
value class FullOverInline(val single: InlineOne)

// CHECK-LABEL: define ptr @"kfun:#testOverInt(){}kotlin.String"
// CHECK: Single#<init>(kotlin.Int){}
// CHECK: Single#<init>(kotlin.Int){}
// CHECK: Single#<init>(kotlin.Int){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: Outer#<init>(Single){}
// CHECK: Outer#<init>(Single){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: Outer#<init>(Single){}
// CHECK: Outer#<init>(Single){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_CALLED_IN_SCOPE: scope=testOverInt function=equals
// CHECK_NEW_COUNT: function=testOverInt count=7 TARGET_BACKENDS=JS_IR
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Single.<init>" inFunction=testOverInt
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Single.equals" inFunction=testOverInt
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Outer.<init>" inFunction=testOverInt
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Outer.equals" inFunction=testOverInt
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
// CHECK: SingleOverUShort#<init>(kotlin.UShort){}
// CHECK: SingleOverUShort#<init>(kotlin.UShort){}
// CHECK: SingleOverUShort#<init>(kotlin.UShort){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: OuterOverUShort#<init>(SingleOverUShort){}
// CHECK: OuterOverUShort#<init>(SingleOverUShort){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK: OuterOverUShort#<init>(SingleOverUShort){}
// CHECK: OuterOverUShort#<init>(SingleOverUShort){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_CALLED_IN_SCOPE: scope=testOverUShort function=equals
// CHECK_NEW_COUNT: function=testOverUShort count=7 TARGET_BACKENDS=JS_IR
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="SingleOverUShort.<init>" inFunction=testOverUShort
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="SingleOverUShort.equals" inFunction=testOverUShort
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="OuterOverUShort.<init>" inFunction=testOverUShort
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="OuterOverUShort.equals" inFunction=testOverUShort
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testOverUShort count=0
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

// The wrapped instances of a full value class are allocated and compared with `equals`. The inline class wrapping them is not boxed
// on JS and Native. On Wasm it is boxed in place for both comparisons, hence four `struct.new` there.
// CHECK-LABEL: define ptr @"kfun:#testInlineOverFull(){}kotlin.String"
// CHECK-NOT: <InlineOverFull-box>
// CHECK-NOT: InlineOverFull#equals
// CHECK-LABEL: epilogue:
// CHECK_CALLED_IN_SCOPE: scope=testInlineOverFull function=equals
// CHECK_NEW_COUNT: function=testInlineOverFull count=4 TARGET_BACKENDS=JS_IR
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Single.<init>" inFunction=testInlineOverFull
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testInlineOverFull count=4
fun testInlineOverFull(): String {
    if (!(InlineOverFull(Single(1)) == InlineOverFull(Single(1)))) return "Fail 9"
    if (InlineOverFull(Single(1)) == InlineOverFull(Single(2))) return "Fail 10"
    return "OK"
}

// The instances of a full value class wrapping an inline class are allocated and compared with `equals`.
// The wrapped inline class is not boxed.
// CHECK-LABEL: define ptr @"kfun:#testFullOverInline(){}kotlin.String"
// CHECK-NOT: <InlineOne-box>
// CHECK: FullOverInline#<init>(InlineOne){}
// CHECK-NOT: <InlineOne-box>
// CHECK: FullOverInline#<init>(InlineOne){}
// CHECK-NOT: <InlineOne-box>
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-NOT: <InlineOne-box>
// CHECK: FullOverInline#<init>(InlineOne){}
// CHECK-NOT: <InlineOne-box>
// CHECK: FullOverInline#<init>(InlineOne){}
// CHECK-NOT: <InlineOne-box>
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-NOT: <InlineOne-box>
// CHECK-LABEL: epilogue:
// CHECK_CALLED_IN_SCOPE: scope=testFullOverInline function=equals
// CHECK_NEW_COUNT: function=testFullOverInline count=4 TARGET_BACKENDS=JS_IR
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="FullOverInline.<init>" inFunction=testFullOverInline
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="FullOverInline.equals" inFunction=testFullOverInline
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=testFullOverInline count=0
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
