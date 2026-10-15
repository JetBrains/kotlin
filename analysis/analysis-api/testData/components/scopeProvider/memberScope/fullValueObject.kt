// LANGUAGE: +FullValueClasses
package test

sealed value class Result {
    abstract val code: Int

    fun isSuccess(): Boolean = code == 0
}

value object Success : Result() {
    override val code: Int get() = 0
}

// class: test/Success
