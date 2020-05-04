// !JVM_TARGET: 1.6

interface B {

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    abstract fun test2(s: String = "")

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    abstract fun test3()


    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    abstract val prop: String

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    abstract val prop2: String

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    val prop3: String
        get() = ""

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@JvmDefault<!>
    var prop4: String
        get() = ""
        set(<!UNUSED_PARAMETER!>value<!>) {}
}
