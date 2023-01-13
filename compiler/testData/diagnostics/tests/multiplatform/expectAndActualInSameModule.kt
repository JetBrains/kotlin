// ISSUE: KT-40904, KT-55177

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class Foo<!> {
    fun memberFun()
    val memberProp: Int
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class Foo<!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 10
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>()
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>x<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>x<!>: String
    get() = "hello"
