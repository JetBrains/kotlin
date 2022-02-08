// !JVM_TARGET: 1.8

interface B {

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    fun test() {}

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    abstract fun test2(s: String = "")

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    abstract fun test3()


    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    abstract val prop: String

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    abstract val prop2: String

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    val prop3: String
        get() = ""

    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    var prop4: String
        get() = ""
        set(value) {}
}
