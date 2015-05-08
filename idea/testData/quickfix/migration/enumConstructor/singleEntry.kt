// "Change to short enum entry super constructor" "true"

enum class SimpleEnum(val z: String) {
    UNIQUE: SimpleEnum("42")<caret>
}