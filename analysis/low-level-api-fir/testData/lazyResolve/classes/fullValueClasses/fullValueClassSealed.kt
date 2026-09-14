// LANGUAGE: +FullValueClasses
package pack

sealed value class Res<caret>ult {
    abstract val code: Int
}

value class Success(val value: String, override val code: Int) : Result()

value object Empty : Result() {
    override val code: Int get() = 0
}
