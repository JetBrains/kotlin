// !LANGUAGE: +NewInference

class B<O>(val obj: O) {
    val v = B(obj)
}
