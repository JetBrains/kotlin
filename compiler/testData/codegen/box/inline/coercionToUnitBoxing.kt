// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=40

class A<T>(val x: T)

fun<T> foo(a: A<T>) = a.x

fun bar(a: A<Int>) {
    foo(a)
}

fun box(): String {
    val a = A(42)
    bar(a)

    return "OK"
}
