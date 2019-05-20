package foo
actual typealias Runnble = java.lang.Runnable

@Suppress("FunctionName")
public actual inline fun Runnble(crossinline block: () -> kotlin.Unit): Runnble = object : Runnble {
    override fun run() {
        block()
    }
}
