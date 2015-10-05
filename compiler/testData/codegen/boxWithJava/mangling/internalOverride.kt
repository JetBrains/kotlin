open class A {
    internal open fun test(): String = "Kotlin"
}

fun box(): String {
    if (A().test() != "Kotlin") return "fail 1: ${A().test()}"

    if (JavaClass().test() != "Java") return "fail 2: ${JavaClass().test()}"

    return "OK"
}