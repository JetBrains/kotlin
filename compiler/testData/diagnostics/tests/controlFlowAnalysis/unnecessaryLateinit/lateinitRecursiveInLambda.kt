// IGNORE_REVERSED_RESOLVE
class Test {
    lateinit var someRunnable: Runnable
    init {
        someRunnable = Runnable { someRunnable.run() }
    }
}
