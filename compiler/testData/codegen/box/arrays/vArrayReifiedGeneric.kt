import java.lang.StringBuilder

// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

inline fun <reified T> foo(p: VArray<T>) = p[0]

@JvmInline
value class IC(val x : Int)

fun box(): String {
    if (foo(VArray(2){42}) != 42) return "Fail"
    return "OK"
}
