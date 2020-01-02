interface IFoo

interface IBar

class A : IFoo, IBar {
    // Unqualified 'super' should be resolved to 'Any'.
    override fun equals(other: Any?): Boolean = super.equals(other)
    override fun hashCode(): Int = super.hashCode()
    override fun toString(): String = super.toString()
}
