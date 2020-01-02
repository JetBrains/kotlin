// !JVM_TARGET: 1.6

interface B {

    @JvmDefault
    fun test() {}

    @JvmDefault
    abstract fun test2(s: String = "")

    @JvmDefault
    abstract fun test3()


    @JvmDefault
    abstract val prop: String

    @JvmDefault
    abstract val prop2: String

    @JvmDefault
    val prop3: String
        get() = ""

    @JvmDefault
    var prop4: String
        get() = ""
        set(value) {}
}
