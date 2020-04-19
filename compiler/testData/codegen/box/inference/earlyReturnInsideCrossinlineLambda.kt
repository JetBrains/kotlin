inline fun <T> runReadAction(crossinline runnable: () -> T): T = runnable()

class Foo {
    fun <K> infer(): K = "OK" as K
}

fun bar(): Int? = 42

fun box(): String {
    return runReadAction {
        bar() ?: return@runReadAction "Failed"
        val foo = Foo()
        foo.infer()
    }
}
