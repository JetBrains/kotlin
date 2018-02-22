// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE
// !API_VERSION: 1.3
// !JVM_TARGET: 1.6
interface B {

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    fun test() {}

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    abstract fun test2(s: String = "")

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    abstract fun test3()



    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    abstract val prop: String

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    abstract val prop2: String

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    val prop3: String
        get() = ""

    <!JVM_DEFAULT_IN_JVM6_TARGET!>@kotlin.annotations.JvmDefault<!>
    var prop4: String
        get() = ""
        set(value) {}
}