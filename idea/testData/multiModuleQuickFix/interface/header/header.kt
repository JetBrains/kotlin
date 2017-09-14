// "Create actual interface for platform JVM" "true"

header interface <caret>Interface {
    fun foo(param: String): Int

    fun String.bar(y: Double): Boolean

    val isGood: Boolean

    var status: Int
}