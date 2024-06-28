// LANGUAGE: -SuspendFunctionsInFunInterfaces

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Test1{
    fun foo()
    fun boo()
}
<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Test2 {}
fun interface Test3 {
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> a: Int
    fun foo()
}
fun interface Test4{
    fun <!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!><T><!> foo(a: T)
}
fun interface Test5{
    fun foo(<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE!>a: Int = 5<!>)
}
fun interface Test6{
    <!FUN_INTERFACE_WITH_SUSPEND_FUNCTION!>suspend<!> fun foo()
}
fun interface Test7{
    fun foo()
}
<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Test8: Test7{
    fun boo()
}
interface Test9 {
    fun num(m: Int): Int {
        return m * m
    }
}
fun interface Test10 : Test9 {
    fun test()
}
interface Test11 {
    val a: Int
}
<!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>fun<!> interface Test12 : Test11 {
    fun test()
}
interface Test14 {
    suspend fun test()
}
<!FUN_INTERFACE_WITH_SUSPEND_FUNCTION!>fun<!> interface Test15 : Test14

