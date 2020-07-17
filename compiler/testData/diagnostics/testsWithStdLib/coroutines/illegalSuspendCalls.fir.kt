import Host.bar

object Host {
    suspend fun bar() {}
}

suspend fun foo() {}

fun noSuspend() {
    foo()
    bar()
}

class A {
    init {
        foo()
        bar()
    }
}

val x = foo()
val y = bar()