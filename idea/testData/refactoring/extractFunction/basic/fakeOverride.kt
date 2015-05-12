interface T {
    fun test() {}
}

fun foo() {
    <selection>(object: T {}).test()</selection>
}