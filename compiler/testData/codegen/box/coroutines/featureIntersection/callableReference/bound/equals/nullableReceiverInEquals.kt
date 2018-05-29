// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_REFLECT

class A

val a = A()
val aa = A()

suspend fun A?.foo() {}

val aFoo = a::foo
val A_foo = A::foo
val nullFoo = null::foo

fun box(): String =
        when {
            nullFoo != null::foo -> "Bound extension refs with same receiver SHOULD be equal"
            nullFoo == aFoo -> "Bound extension refs with different receivers SHOULD NOT be equal"
            nullFoo == A_foo -> "Bound extension ref with receiver 'null' SHOULD NOT be equal to free ref"

            else -> "OK"
        }
