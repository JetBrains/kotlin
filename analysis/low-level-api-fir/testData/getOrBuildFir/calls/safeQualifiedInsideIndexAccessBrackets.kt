class A {
    operator fun get(key: Boolean) = true
    fun self() = this
}

fun test(a: A?) {
    a?.self()<expr>[false]</expr>
}