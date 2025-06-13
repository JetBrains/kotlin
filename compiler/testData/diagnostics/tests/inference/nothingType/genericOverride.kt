// RUN_PIPELINE_TILL: BACKEND
interface A {
    fun a(): A
}

fun error(s: String): Nothing = null!!

class A1 : A {
    override fun a() = <!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>test<!>() ?: error("")

    @Suppress("UNCHECKED_CAST")
    private fun <V : A> test(): V? = this as? V
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, elvisExpression, functionDeclaration, interfaceDeclaration,
nullableType, override, stringLiteral, thisExpression, typeConstraint, typeParameter */
