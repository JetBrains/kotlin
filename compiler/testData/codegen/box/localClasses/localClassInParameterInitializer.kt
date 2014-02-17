class A(
        val a: String = {
            open class B() {
                open fun s() : String = "O"
            }

            object O: B() {
                override fun s(): String = "K"
            }

            B().s() + O.s()
        }()
)

fun box() : String {
    return A().a
}