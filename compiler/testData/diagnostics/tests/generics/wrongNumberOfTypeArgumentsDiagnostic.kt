// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun myFun(i : String) {}
fun myFun(i : Int) {}

fun test1() {
    <!NONE_APPLICABLE!>myFun<!><Int>(3)
    <!NONE_APPLICABLE!>myFun<!><String>('a')
}

fun test2() {
    val m0 = java.util.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>HashMap<!>()
    val m1 = java.util.HashMap<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String, String><!>()
    val m2 = java.util.HashMap<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>()
}
