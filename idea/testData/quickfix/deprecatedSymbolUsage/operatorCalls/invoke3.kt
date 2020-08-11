// "Replace with 'execute(action)'" "true"

class Executor {
    @Deprecated("Use Executor.execute(Runnable) instead.", ReplaceWith("execute(action)"))
    operator fun invoke(action: () -> Unit) {}

    fun execute(action: () -> Unit) {}

    fun usage(executor: Executor) {
        <caret>invoke {
            // do something
        }
    }
}