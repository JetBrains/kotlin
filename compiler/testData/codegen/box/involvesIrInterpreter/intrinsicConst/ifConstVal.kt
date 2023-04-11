// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JVM_IR, NATIVE, JS_IR, JS_IR_ES6

const val flag = <!EVALUATED("true")!>true<!>
const val value = <!EVALUATED("10")!>10<!>
const val condition = <!EVALUATED("True")!>if (flag) "True" else "Error"<!>
const val withWhen = <!EVALUATED("True")!>when (flag) { true -> "True"; else -> "Error" }<!>
const val withWhen2 = <!EVALUATED("True")!>when { flag == true -> "True"; else -> "Error" }<!>
const val withWhen3 = <!EVALUATED("1")!>when(value) { 10 -> "1"; 100 -> "2"; else -> "3" }<!>
const val multibranchIf = <!EVALUATED("3")!>if (value == 100) 1 else if (value == 1000) 2 else 3<!>

fun box(): String {
    if (<!EVALUATED("false")!>condition != "True"<!>) return "Fail 1"
    if (<!EVALUATED("false")!>withWhen != "True"<!>) return "Fail 2"
    if (<!EVALUATED("false")!>withWhen2 != "True"<!>) return "Fail 3"
    if (<!EVALUATED("false")!>withWhen3 != "1"<!>) return "Fail 4"
    if (<!EVALUATED("false")!>multibranchIf != 3<!>) return "Fail 5"
    return "OK"
}
