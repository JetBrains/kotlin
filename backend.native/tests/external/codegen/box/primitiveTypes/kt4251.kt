fun box(): String {
    val a: Char? = 'a'
    val result = a!! < 'b'
    return if (result) "OK" else "Fail"
}
