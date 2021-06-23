// !JVM_TARGET: 1.8

abstract class A {

    @<!DEPRECATION!>JvmDefault<!>
    fun test() {}

    @<!DEPRECATION!>JvmDefault<!>
    abstract fun test2(s: String = "")

    @<!DEPRECATION!>JvmDefault<!>
    abstract fun test3()
}

object B {

    @<!DEPRECATION!>JvmDefault<!>
    fun test() {}

    @<!DEPRECATION!>JvmDefault<!>
    fun test2(s: String = "") {}
}
