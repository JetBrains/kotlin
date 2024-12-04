class A

fun Any.test(): () -> Int = when {
    this is A -> fun() = <expr>e</expr>
    else -> { { 0 } }
}
