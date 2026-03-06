// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// WITH_STDLIB

@JvmInline
value class Test(val s: String, val s1: String) {
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