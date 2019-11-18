// IGNORE_BACKEND_FIR: JVM_IR
class MyQueue {
    fun poll(): String? =  null
}

class A {
    val delayedQueue = MyQueue()

    fun next() {
        while (true) {
            delayedQueue.poll() ?: break
        }

        while (true) {
            unblock(delayedQueue.poll() ?: break)
        }

        while (true) {
            unblock(delayedQueue.poll() ?: break)
        }
    }

    fun unblock(p: String) {

    }
}

fun box() : String {
    A().next()
    return "OK"
}