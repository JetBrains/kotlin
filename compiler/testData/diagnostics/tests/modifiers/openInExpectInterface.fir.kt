// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
// TODO: .fir.kt version is just a stub.

expect interface My {
    open fun bar()
    <!EXPECTED_DECLARATION_WITH_BODY!>open fun bas()<!> {}
    <!REDUNDANT_MODIFIER!>open<!> abstract fun bat(): Int

    fun foo()


    open val a: Int
    open val b: String
    open val c: String get() = ""
    <!REDUNDANT_MODIFIER!>open<!> abstract val e: Int

    val f: Int
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MyImpl1<!>: My

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MyImpl2<!>: My {
    override fun foo() {}
    override val f = 0
    override val e = 1
}

expect interface Outer {
    interface Inner {
        open fun bar()
        <!EXPECTED_DECLARATION_WITH_BODY!>open fun bas()<!> {}
        <!REDUNDANT_MODIFIER!>open<!> abstract fun bat(): Int
        fun foo()
    }
}
