// ISSUE: KT-91357
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// FILECHECK_STAGE: CStubs
// Checks that the representation of a value class does not depend on the module it is used from: off JVM, a single-field full
// value class from another module is compared like an inline class: without allocations and calls of `equals` on JS and Native,
// and with boxing in place on Wasm, which unwraps only inline classes when comparing (KT-86967).

// MODULE: lib
// FILE: lib.kt
value class Full(val x: Int)

inline value class Inline(val x: Int)

inline fun <T> same(a: T, b: T): Boolean = a == b

// MODULE: main(lib)
// FILE: main.kt
// CHECK-LABEL: define {{.*}}@"kfun:#fullEquals(kotlin.Int;kotlin.Int){}kotlin.Boolean"
// CHECK-NOT: <Full-box>
// CHECK-NOT: #equals(kotlin.Any?){}kotlin.Boolean
// CHECK-LABEL: epilogue:
// CHECK_NOT_CALLED_IN_SCOPE: scope=fullEquals function=equals
// CHECK_NEW_COUNT: function=fullEquals count=0 TARGET_BACKENDS=JS_IR
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=struct.new inFunction=fullEquals count=2
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
