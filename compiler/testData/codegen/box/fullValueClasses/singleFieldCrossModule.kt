// ISSUE: KT-91357
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// Checks that the representation of a value class does not depend on the module it is used from: a single-field full
// value class from another module is allocated and compared with `equals`, an inline class is compared without them.

// MODULE: lib
// FILE: lib.kt
value class Full(val x: Int)

inline value class Inline(val x: Int)

inline fun <T> same(a: T, b: T): Boolean = a == b

// MODULE: main(lib)
// FILE: main.kt
// CHECK-LABEL: define {{.*}}@"kfun:#fullEquals(kotlin.Int;kotlin.Int){}kotlin.Boolean"
// CHECK: Full#<init>(kotlin.Int){}
// CHECK: Full#<init>(kotlin.Int){}
// CHECK: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_CALLED_IN_SCOPE: scope=fullEquals function=equals
// CHECK_NEW_COUNT: function=fullEquals count=2 TARGET_BACKENDS=JS_IR
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Full.<init>" inFunction=fullEquals
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled="Full.equals" inFunction=fullEquals
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=fullEquals count=0
fun fullEquals(x: Int, y: Int): Boolean = Full(x) == Full(y)

// CHECK-LABEL: define {{.*}}@"kfun:#inlineEquals(kotlin.Int;kotlin.Int){}kotlin.Boolean"
// CHECK-NOT: <Inline-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=inlineEquals function=equals
// CHECK_NEW_COUNT: function=inlineEquals count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=inlineEquals count=0
fun inlineEquals(x: Int, y: Int): Boolean = Inline(x) == Inline(y)

fun box(): String {
    if (!fullEquals(1, 1)) return "Fail 1"
    if (fullEquals(1, 2)) return "Fail 2"
    if (!inlineEquals(1, 1)) return "Fail 3"
    if (inlineEquals(1, 2)) return "Fail 4"
    if (!same(Full(1), Full(1))) return "Fail 5"
    if (same(Full(1), Full(2))) return "Fail 6"
    if (!same(Inline(1), Inline(1))) return "Fail 7"
    if (same(Inline(1), Inline(2))) return "Fail 8"
    return "OK"
}
