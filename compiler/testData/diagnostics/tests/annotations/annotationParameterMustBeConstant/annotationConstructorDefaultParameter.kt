const val iConst = 42
val iVal = 42
fun iFun() = 42

annotation class Ann(val x: Int)
annotation class Test1(val x: Int = 42)
annotation class Test2(val x: Int = iConst)
annotation class Test3(val x: Int = 1 + iConst + 1)
annotation class Test4(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>iVal<!>)
annotation class Test5(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>1 + iVal + 1<!>)
annotation class Test6(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>iFun()<!>)
annotation class Test7(val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>1 + iFun() + 1<!>)
