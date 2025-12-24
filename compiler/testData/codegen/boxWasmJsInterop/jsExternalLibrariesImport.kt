// FILE: sqrt.kt
@file:JsQualifier("Math")
package math
external fun sqrt(x: Double): Double


// FILE: main.kt
fun box(): String {
    if (math.sqrt(4.0) != 2.0) return "sqrt(4.0) should be 2.0"
    return "OK"
}