// !JVM_TARGET: 1.8

abstract class A {

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    abstract fun test2(s: String = "")

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    abstract fun test3()
}

object B {

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    fun test2(<!UNUSED_PARAMETER!>s<!>: String = "") {}
}
