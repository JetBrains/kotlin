// !DIAGNOSTICS: -UNUSED_PARAMETER

object Delegate {
    operator fun getValue(x: Any?, y: Any?): String = ""
}

fun <T> delegateFactory(p: Any) = Delegate

class C(p: Any, val v: Any) {

    val test1 get() = p

    val test2 get() = v

    // NB here we can use both 'T' (property type parameter) and 'p' (primary constructor parameter)
    val <T> List<T>.test3 by delegateFactory<T>(p)

    val test4 get() { return p }

    var test5
        get() { return p }
        set(nv) { p.let {} }
}