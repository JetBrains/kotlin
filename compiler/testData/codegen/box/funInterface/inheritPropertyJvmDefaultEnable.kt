// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: enable

// IGNORE_BACKEND: JVM_IR
// ^ KT-68452 ClassFormatError: Extra method is generated when functional interface extends ordinary one

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
