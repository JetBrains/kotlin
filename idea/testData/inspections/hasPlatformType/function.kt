// WITH_RUNTIME

fun foo() = java.lang.String.valueOf(1)

private fun foo() = java.lang.String.valueOf(2)

open class My {
    protected fun foo() = java.lang.String.valueOf(3)
}