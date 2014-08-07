class A {
    public fun iterator(): Iterator<String> = throw IllegalStateException("")
}

fun test() {
    for (a in A<String>()) {}
    a.iterator()
}