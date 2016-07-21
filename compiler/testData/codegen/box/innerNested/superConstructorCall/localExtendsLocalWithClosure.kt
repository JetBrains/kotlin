fun box(): String {
    val result = "OK"

    open class Local(val ok: Boolean) {
        fun result() = if (ok) result else "Fail"
    }

    class Derived : Local(true)

    return Derived().result()
}
