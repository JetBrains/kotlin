annotation class Anno

<expr>@receiver:Anno</expr>
fun String.foo() {
    return "$this (${this.length})"
}
