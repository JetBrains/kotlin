interface Runnable {
    fun run()
}

class C {
    fun f() {
        class MyRunnable(): Runnable {
        }
    }
}