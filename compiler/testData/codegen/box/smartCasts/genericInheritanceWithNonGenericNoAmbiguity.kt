// ISSUE: KT-55705

interface A<T> {
    fun foo(x: T?) {}
}

interface B : A<String> {
    override fun foo(x: String?)
}

fun <T> bar(x: A<in T>) {
    if (x is B) {
        // The code should be green
        x.foo(null)
    }
}

fun box(): String {
    bar<String>(
        object : B {
            override fun foo(x: String?) {}
        }
    )
    return "OK"
}
