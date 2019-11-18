// IGNORE_BACKEND_FIR: JVM_IR
class A(
        val a: String = {
            open class B() {
                open fun s() : String = "O"
            }

            val o = object : B() {
                override fun s(): String = "K"
            }

            B().s() + o.s()
        }()
)

fun box() : String {
    return A().a
}