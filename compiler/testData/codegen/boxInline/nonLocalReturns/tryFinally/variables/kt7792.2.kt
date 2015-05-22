package test

inline fun <R> mySynchronized(lock: Any, block: () -> R): R {
    monitorCall(lock)
    try {
        return block()
    }
    finally {
        monitorCall(lock)
    }
}

fun monitorCall(lock: Any) {

}