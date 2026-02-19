// RUN_PIPELINE_TILL: BACKEND
interface A<D : A<D>> {
    fun foo(): Any

    val cond: Boolean
    val field: Any
}

interface B<F : B<F>> : A<F> {
    override fun foo(): CharSequence
}

interface C : B<C> {
    override fun foo(): String
}

fun test(x: A<*>) {
    when ((x as? C)?.field) {
        is String -> {
            if ((x as? B)?.cond == true) {
                x.foo()
            }
        }
    }

}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, override, propertyDeclaration, safeCall, smartcast, starProjection, typeConstraint, typeParameter,
whenExpression, whenWithSubject */
