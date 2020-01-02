// Functions can be recursively annotated
annotation class ann(val x: Int)
@ann(bar()) fun foo() = 1
@ann(foo()) fun bar() = 2