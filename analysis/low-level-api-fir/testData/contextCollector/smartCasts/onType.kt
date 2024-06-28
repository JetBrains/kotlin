fun test(a: Any) {
    if (a !is Foo) {
        return
    }

    call<List<<expr>Int</expr>>>()
}

inline fun <reified T : Any> call() {}

interface Foo {
    fun process(): Boolean
}