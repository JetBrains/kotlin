// WITH_STDLIB

// FILE: lib.kt
fun interface MyRunnable {
    fun invoke()
}

class A {
    inline fun doWork(noinline job: () -> Unit) {
        MyRunnable(job).invoke()
    }

    fun doNoninlineWork(job: () -> Unit) {
        MyRunnable(job).invoke()
    }
}

// FILE: main.kt
fun box(): String {
    var result = false
    A().doWork { result = true }
    if (!result) return "Fail 1"

    result = false
    A().doNoninlineWork { result = true }
    if (!result) return "Fail 2"

    return "OK"
}
