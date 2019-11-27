// IGNORE_BACKEND_FIR: JVM_IR
class MyQueue {
    fun poll(): String? =  null
}

class A {
    val delayedQueue = MyQueue()

    var cond = true

    fun next() {
        while (cond) {
            delayedQueue.poll() ?: break
        }

        while (cond) {
            unblock(delayedQueue.poll() ?: break)
        }

        while (cond) {
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