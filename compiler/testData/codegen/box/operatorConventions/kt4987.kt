fun box(): String {
    fun Int?.inc() = (this ?: 0) + 1
    var counter: Int? = null
    counter++
    return if (counter == 1) "OK" else "fail: $counter"
}