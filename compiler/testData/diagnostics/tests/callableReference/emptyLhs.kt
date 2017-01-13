// !DIAGNOSTICS: -UNUSED_EXPRESSION, -EXTENSION_SHADOWED_BY_MEMBER

val topLevelVal = 1
fun topLevelFun() = 2

val A.extensionVal: Int get() = 3
fun A.extensionFun(): Int = 4

class A {
    val memberVal = 5
    fun memberFun() = 6

    val ok1 = ::topLevelVal
    val ok2 = ::topLevelFun

    fun fail1() {
        ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>extensionVal<!>
        ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>extensionFun<!>
    }

    fun fail2() {
        ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>memberVal<!>
        ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>memberFun<!>
    }
}



val ok1 = ::topLevelVal
val ok2 = ::topLevelFun

fun A.fail1() {
    ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>extensionVal<!>
    ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>extensionFun<!>
}

fun A.fail2() {
    ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>memberVal<!>
    ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>memberFun<!>
}
