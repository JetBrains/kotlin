class Test {
    lateinit var someRunnable: Runnable
    init {
        someRunnable = Runnable { someRunnable.run() }
    }
}
