// "Change to short enum entry super constructor" "true"

enum class SimpleEnum(val z: String = "xxx") {
    FIRST(),
    SECOND: SimpleEnum("42")<caret>,
    LAST("13");

    fun foo() = z
}