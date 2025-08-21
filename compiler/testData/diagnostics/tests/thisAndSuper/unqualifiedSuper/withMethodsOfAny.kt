// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface IFoo

interface IBar

class A : IFoo, IBar {
    // Unqualified 'super' should be resolved to 'Any'.
    override fun equals(other: Any?): Boolean = super.equals(other)
    override fun hashCode(): Int = super.hashCode()
    override fun toString(): String = super.toString()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, operator, override,
superExpression */
