// Can't be tested in JVM because frontend doesn't allow such code
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// WITH_STDLIB

package java.lang

object Math {
    const val E: Double = kotlin.math.<!EVALUATED("2.718281828459045")!>E<!>
    const val PI: Double = kotlin.math.<!EVALUATED("3.141592653589793")!>PI<!>
    const val OK: String = <!EVALUATED("OK")!>"OK"<!>
}

const val usageE = Math.<!EVALUATED("2.718281828459045")!>E<!>
const val usagePI = Math.<!EVALUATED("3.141592653589793")!>PI<!>

fun box(): String {
    return Math.<!EVALUATED("OK")!>OK<!>
}
