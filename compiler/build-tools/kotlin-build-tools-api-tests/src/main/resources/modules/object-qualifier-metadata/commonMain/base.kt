class A

object Holder {
    val name: String get() = "holder"

    fun select(x: A): String = "common"
}
