// !DIAGNOSTICS: -UNUSED_EXPRESSION, -EXTENSION_SHADOWED_BY_MEMBER
// !LANGUAGE: +CallableReferencesToClassMembersWithEmptyLHS

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
        ::extensionVal
        ::extensionFun
    }

    fun fail2() {
        ::memberVal
        ::memberFun
    }
}



val ok1 = ::topLevelVal
val ok2 = ::topLevelFun

fun A.fail1() {
    ::extensionVal
    ::extensionFun
}

fun A.fail2() {
    ::memberVal
    ::memberFun
}
