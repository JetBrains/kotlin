interface Flusher {
    fun flush() = "OK"
}

fun myFlusher() = object : Flusher { }

fun flushIt(flusher: Flusher) = flusher.flush()

fun box() = flushIt(myFlusher())
