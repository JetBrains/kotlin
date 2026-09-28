// LANGUAGE: +FullValueClasses
package pack

sealed value class Result {
    abstract val code: Int
}

value class Success(val value: String, override val code: Int) : Result()

value object Emp<caret>ty : Result() {
    override val code: Int get() = 0
}
