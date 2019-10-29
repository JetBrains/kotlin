// "Replace with 'execute(action)'" "true"

@Deprecated("Use Executor.execute(Runnable) instead.", ReplaceWith("execute(action)"))
public operator fun Executor.invoke(action: () -> Unit) {}

class Executor {
    fun execute(action: () -> Unit) {}
}

fun usage(executor: Executor) {
    <caret>executor {
        // do something
    }
}