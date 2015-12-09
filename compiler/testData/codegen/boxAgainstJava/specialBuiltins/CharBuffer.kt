fun box(): String {
    val cb: CharBuffer = CharBuffer.impl()

    return cb.get(0).toString() + (cb as CharSequence).get(1).toString()
}
