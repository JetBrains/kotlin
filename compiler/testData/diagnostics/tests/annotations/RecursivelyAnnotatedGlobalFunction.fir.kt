// Functions can be recursively annotated
annotation class ann(val x: Int)
@ann(foo()) fun foo() = 1