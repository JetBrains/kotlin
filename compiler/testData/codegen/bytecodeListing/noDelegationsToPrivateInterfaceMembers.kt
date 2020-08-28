interface A {
    // There must be no delegation methods for 'log' and 'bar' in C as they are private
    private val log: String get() = "O"
    private fun bar() = "K"

    fun foo() = log + bar()
}

interface B : A
class C : B
