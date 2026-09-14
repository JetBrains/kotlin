// LANGUAGE: +FullValueClasses
sealed value class Result {
    abstract val code: Int
}

value object Empty : Result() {
    override val code: Int get() = 0
}
