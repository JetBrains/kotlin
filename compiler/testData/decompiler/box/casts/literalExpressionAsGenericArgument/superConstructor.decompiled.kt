open  class Base<T>(val value: T) {
}
class Box: Base<T> (-1) {
}
fun box() : String  {
    val expected : Long? = -1
    return if (Box().value == expected) {
    "OK"
}
else {
    "fail"
}
}
