import test.*

class A: P() {
    override val FOO: String
        get() = "fail"

    override fun test(): String {
        return "fail"
    }
}

fun box() : String {
    val p = P()
    return p.protectedProp() + p.protectedFun()
}