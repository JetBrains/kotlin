// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// COMMON_COROUTINES_TEST
// WITH_REFLECT

import kotlin.reflect.full.*

class C {
    suspend fun foo() {}
}

val C_fooReflect = C::class.functions.find { it.name == "foo" }!!
val C_foo = C::foo
val cFoo = C()::foo

val Any.className: String
        get() = this::class.qualifiedName!!

fun box(): String =
        when {
            C_fooReflect != C_foo -> "C_fooReflect != C_foo, ${C_fooReflect.className}"
            C_foo != C_fooReflect -> "C_foo != C_fooReflect, ${C_foo.className}"
            C_fooReflect == cFoo -> "C_fooReflect == cFoo, ${C_fooReflect.className}"
            cFoo == C_fooReflect -> "cFoo == C_fooReflect, ${cFoo.className}"
            else -> "OK"
        }
