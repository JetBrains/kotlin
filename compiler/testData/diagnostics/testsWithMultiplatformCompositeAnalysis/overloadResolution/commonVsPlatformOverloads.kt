// MODULE: m1-common
// FILE: common.kt

package pkg

fun overloaded(arg: Double = 1.5) {}
expect fun overloaded(arg: Int = 15)

expect fun justExpectActual()

fun common() {
    overloaded()
    justExpectActual()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package pkg

actual fun overloaded(arg: Int) {}
actual fun justExpectActual() {}

fun platoform() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>overloaded<!>()
    justExpectActual()
}
