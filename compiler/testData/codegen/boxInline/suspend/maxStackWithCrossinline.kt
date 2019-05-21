// IGNORE_BACKEND: JVM_IR
// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

fun handle(f: suspend () -> Unit) {}

open class Foo {
    inline fun foo(crossinline body: suspend (Baz) -> Unit, crossinline createContext: () -> Baz) {
        handle {
            body(createContext())
        }
    }
}

class Bar : Foo() {
    inline fun bar(crossinline body: suspend (Baz) -> Unit) {
        this.foo(body) {
            Baz(Unit)
        }
    }
}

class Baz(unit: Unit)

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

fun box(): String {
    Bar().bar {}
    return "OK"
}
