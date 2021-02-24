// !LANGUAGE: +InlineClasses

fun <T> foo(a: IC): T = a.value as T

inline class IC(val value: String)

fun box(): String {
    return foo<String>(IC("O")) + "K"
}
