// Result type can be annotated
annotation class My(val x: Int)

fun foo(): @My(42) Int = 24