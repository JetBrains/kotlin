package test

public inline fun <R> mysynchronized(lock: Any, block: () -> R): R {
    try {
        return block()
    }
    finally {
        //do nothing
        1
    }
}