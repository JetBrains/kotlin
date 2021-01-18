inline class Test(val s: String) {
    fun memberFun(x: String) = s

    fun String.memberExtFun() = s

    val memberVal
        get() = s

    val String.memberExtVal
        get() = s

    var memberVar
        get() = s
        set(value) {}

    var String.memberExtVar
        get() = s
        set(value) {}
}