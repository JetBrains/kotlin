class A

context(a: A)
operator fun A.invoke() {}

context(_: A)
fun usage() {
    val a = A()
    <expr>a()</expr>
}

// LANGUAGE: +ContextParameters
