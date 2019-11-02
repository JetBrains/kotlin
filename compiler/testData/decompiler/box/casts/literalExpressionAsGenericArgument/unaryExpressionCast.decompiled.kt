class Box<T>(val value: T) {
}
fun box() : String  {
    val b : Box<Long> = Box<Long>< Long >(-1)
    val expected : Long? = -1
    return if (b.value == expected) {
    "OK"
}
else {
    "fail"
}
}
