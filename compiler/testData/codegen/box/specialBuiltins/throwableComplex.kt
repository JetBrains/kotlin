open class Base(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

open class Base2(message: String? = null, cause: Throwable? = null): Base(message, cause)

open class Override(message: String? = null, cause: Throwable? = null) : Base2(message, cause) {

    var i = 0

    override val message: String?
        get() = "Override: " + super.message + "${i++}"
}

open class OverBase(message: String? = null, cause: Throwable? = null): Override(message, cause)

open class OverOverride(message: String? = null, cause: Throwable? = null) : OverBase(message, cause) {

    override val message: String?
        get() = "OverOver: " + super.message + "${i++}"

    override val cause: Throwable?
        get() = super.cause ?: this

}


fun box(): String {
    check(Base("O", Base("K")), "OK")
    check(Override("OK"), "Override: OK0")
    check(OverOverride("OK"), "OverOver: Override: OK01OverOver: Override: OK23")
    return "OK"
}


fun check(t: Throwable, msg: String) {
    try {
        throw t
    } catch (e: Throwable) {
        val c = t.cause
        val m = if (c != null) t.message!! + c.message!! else t.message!!
        if (m != msg) throw AssertionError(m)
    }
}