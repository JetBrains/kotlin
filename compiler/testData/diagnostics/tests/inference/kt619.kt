// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A(t : Int) : Comparable<A> {
    var i = t
    override fun compareTo(other : A) = (this.i - other.i)
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, operator, override, primaryConstructor,
propertyDeclaration, thisExpression */
