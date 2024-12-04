// WITH_STDLIB
package test

class Target<T> {
    fun add(t: T) {}
}

fun <T> buildTarget(@BuilderInference block: Target<T>.() -> Unit): Target<T> {
    return Target<T>().apply { block() }
}

fun test(s: String) {
    val result = buildTarget { <expr>add</expr>(s) }
}
