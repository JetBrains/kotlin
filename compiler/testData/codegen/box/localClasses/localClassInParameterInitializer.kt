fun <T> eval(fn: () -> T) = fn()

class A(
        val a: String = eval {
            open class B() {
                open fun s() : String = "O"
            }

            val o = object : B() {
                override fun s(): String = "K"
            }

            B().s() + o.s()
        }
)

fun box() : String {
    return A().a
}