// Function parameter CAN be recursively annotated
annotation class ann(val x: Int)
fun foo(@ann(foo(1)) x: Int): Int = x