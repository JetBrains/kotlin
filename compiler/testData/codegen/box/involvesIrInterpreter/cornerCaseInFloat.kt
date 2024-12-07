// TARGET_BACKEND: JS_IR

// This test can be dropped when we will make a proper design how to evaluate constants on different platforms.

// On runtime JVM it will be "0.3"
// On runtime JS it will be "0.30000000000000004"
const val a = <!EVALUATED("0.3")!>0.1f + 0.2f<!>
val number = 0.1f

fun box(): String {
    val b = 0.1f + 0.2f
    val c = number + 0.2f

    if (<!EVALUATED{IR}("0.3")!>a<!>.toString() == b.toString()) return "Fail 1: unexpected optimization"
    if (<!EVALUATED{IR}("0.3")!>a<!>.toString() == c.toString()) return "Fail 2: unexpected equality of compile time and runtime results"

    return "OK"
}
