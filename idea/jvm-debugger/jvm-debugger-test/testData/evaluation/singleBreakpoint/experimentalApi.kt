package experimentalApi

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class UnstableApi

@UnstableApi
fun foo() = 5

fun main() {
    //Breakpoint!
    val a = 5
}

// EXPRESSION: foo()
// RESULT: 5: I