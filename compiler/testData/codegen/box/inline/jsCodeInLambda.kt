// TARGET_BACKEND: JS_IR, JS_IR_ES6
// FILE: lib.kt
package foo

inline fun foo(action: (x: Int, y: Int) -> Int): Int = action(1, 2)

// FILE: main.kt
package foo

val bar = foo { x, y -> js("x + y") }

fun box(): String {
    assertEquals(3, bar)

    return "OK"
}
