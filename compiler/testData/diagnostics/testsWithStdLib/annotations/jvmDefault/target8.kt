// FIR_IDENTICAL
// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface B {

    @<!DEPRECATION!>JvmDefault<!>
    fun test() {}

    @<!DEPRECATION!>JvmDefault<!>
    abstract fun test2(s: String = "")

    @<!DEPRECATION!>JvmDefault<!>
    abstract fun test3()


    @<!DEPRECATION!>JvmDefault<!>
    abstract val prop: String

    @<!DEPRECATION!>JvmDefault<!>
    abstract val prop2: String

    @<!DEPRECATION!>JvmDefault<!>
    val prop3: String
        get() = ""

    @<!DEPRECATION!>JvmDefault<!>
    var prop4: String
        get() = ""
        set(value) {}
}
