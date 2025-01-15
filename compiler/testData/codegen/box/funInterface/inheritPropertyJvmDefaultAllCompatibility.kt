// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: all-compatibility

interface Foo<T> {
    val T.prop: String
        get() = "fail"
}

fun interface F : Foo<String> {
    fun invoke(o: String): String
}

fun box(): String {
    if (F { o -> o + "K" }.invoke("O") != "OK") return "Fail"

    val lambda: (String) -> String = { o -> o + "K" }
    return F(lambda).invoke("O")
}
