// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE
// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
interface B {

    @kotlin.annotations.JvmDefault
    fun test() {}

    @kotlin.annotations.JvmDefault
    abstract fun test2(s: String = "")

    @kotlin.annotations.JvmDefault
    abstract fun test3()


    @kotlin.annotations.JvmDefault
    abstract val prop: String

    @kotlin.annotations.JvmDefault
    abstract val prop2: String

    @kotlin.annotations.JvmDefault
    val prop3: String
        get() = ""

    @kotlin.annotations.JvmDefault
    var prop4: String
        get() = ""
        set(value) {}
}