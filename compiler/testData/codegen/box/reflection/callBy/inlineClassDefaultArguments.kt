// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

inline class A(val x: Int)

fun test(x: A = A(0)) = "OK"

fun box(): String {
    return (::test).callBy(mapOf())
}
