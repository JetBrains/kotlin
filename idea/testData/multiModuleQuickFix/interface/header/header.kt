// "Create actual interface for module jvm (JVM)" "true"

expect interface <caret>Interface {
    fun foo(param: String): Int

    fun String.bar(y: Double): Boolean

    val isGood: Boolean

    var status: Int

    class Nested {
        fun bar()
    }
}