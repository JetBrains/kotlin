// WITH_RUNTIME
// TARGET_BACKEND: JVM

class Foo<T>(val x: Int)

@JvmSuppressWildcards
class Bar {
    fun run(f: (Foo<String>) -> Foo<Long>): Foo<Long> {
        return f(Foo<String>(42))
    }

    fun invokeFun(): Foo<Long> {
        return run { f ->
            Foo<Long>(f.x + 1)
        }
    }

    fun nullableFoo(f: Foo<Long>?): Foo<Long> = f!!

    fun listOfFoo(f: List<Foo<String>>): Foo<String> = f[0]
}

fun box(): String {
    val b = Bar()
    if(b.invokeFun().x != 43) return "Fail 1"

    if (b.nullableFoo(Foo<Long>(1)).x != 1) return "Fail 2"

    val f: Foo<Long>? = Foo<Long>(2)
    if (b.nullableFoo(f).x != 2) return "Fail 3"

    val ls = listOf(Foo<String>(3))
    if (b.listOfFoo(ls).x != 3) return "Fail 4"

    return "OK"
}