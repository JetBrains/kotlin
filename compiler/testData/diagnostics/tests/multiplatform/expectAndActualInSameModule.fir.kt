// ISSUE: KT-40904, KT-55177

expect class Foo {
    fun memberFun()
    val memberProp: Int
}
actual class Foo {
    <!ACTUAL_WITHOUT_EXPECT!>actual fun memberFun() {}<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual val memberProp: Int = 10<!>
}

expect fun foo()
<!ACTUAL_WITHOUT_EXPECT!>actual fun foo() {}<!>

expect val x: String
<!ACTUAL_WITHOUT_EXPECT!>actual val x: String
    get() = "hello"<!>
