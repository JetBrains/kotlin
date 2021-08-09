class Base {
    val x: CharSequence
        internal field: String = "OK"

}
val s: String = Base().x
fun box(): String {
    return s
}
