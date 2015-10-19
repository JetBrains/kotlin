// FORCED
open class X {
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

class A : X() {<caret>
    fun foo() {

    }
}