// !LANGUAGE: +InlineClasses

// FILE: Test.kt

inline fun <R, T> foo(x : R, y : R, block : (R) -> T) : T {
    val a = x is Number
    val b = x is Object

    val a1 = x as Number
    val b1 = x as Object

    if (a && b) {
        return block(x)
    } else {
        return block(y)
    }
}

fun bar() {
    foo(1, 2) { x -> x is Int }
}

// @TestKt.class:
// 0 valueOf
// 0 Value\s\(\)
// 2 INSTANCEOF
// 1 CHECKCAST

// FILE: Inline.kt

inline class InlinedInt(val x: Int)

// FILE: TestInlined.kt

fun baz() {
    foo(InlinedInt(1), InlinedInt(2)) { x -> x is InlinedInt }
}

// @TestInlinedKt.class:
// 0 valueOf
// 0 Value\s\(\)
// 0 INSTANCEOF
// 0 CHECKCAST
// 0 INVOKESTATIC InlinedInt\$Erased.box
// 0 INVOKEVIRTUAL InlinedInt.unbox
