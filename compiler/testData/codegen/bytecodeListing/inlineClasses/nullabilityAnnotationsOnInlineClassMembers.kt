// WITH_STDLIB
inline class Test(val s: String) {
    fun memberFun(x: String) = s

    fun String.memberExtFun() = s

    val memberVal
        get() = s

    val String.memberExtVal
        get() = s

    @Suppress("RESERVED_VAR_PROPERTY_OF_VALUE_CLASS")
    var memberVar
        get() = s
        set(value) {}

    @Suppress("RESERVED_VAR_PROPERTY_OF_VALUE_CLASS")
    var String.memberExtVar
        get() = s
        set(value) {}
}