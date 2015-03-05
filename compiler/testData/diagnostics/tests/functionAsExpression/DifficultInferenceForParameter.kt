// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> foo(f : (T) -> T) : T = throw Exception()

fun test() {
    val a : Int = foo(fun f(x) = x)
}
