// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

inline class Foo(val arg: String)

fun box(): String {
    val ls = listOf(Foo("abc"), Foo("def"))
    var res = ""
    for (el in ls) {
        res += el.arg
    }

    return if (res != "abcdef") "Fail" else "OK"
}
