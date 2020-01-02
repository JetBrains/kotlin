open class S(val a: Any, val b: Any, val c: Any) {}

object A : S(prop1, prop2, func()) {
    val prop1 = 1
    val prop2: Int
        get() = 1
    fun func() {}
}
