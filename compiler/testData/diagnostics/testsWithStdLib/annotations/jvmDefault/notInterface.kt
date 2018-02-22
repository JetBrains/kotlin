// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE
// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
abstract class A {

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@kotlin.annotations.JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@kotlin.annotations.JvmDefault<!>
    abstract fun test2(s: String = "")

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@kotlin.annotations.JvmDefault<!>
    abstract fun test3()
}

object B {

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@kotlin.annotations.JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@kotlin.annotations.JvmDefault<!>
    fun test2(<!UNUSED_PARAMETER!>s<!>: String = "") {}
}