// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-73779

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

class A(val a: String = "d ") {

    context(a: A)
    fun A.funMember(): String {
        return a.a + this@A.a + this.a
    }

    context(a: A)
    val A.propertyMember: String
        get() = a.a + this@A.a + this.a

    fun simpleUsageInsideClass(): String {
        return funMember() + propertyMember
    }

    fun usageWithThisInsideClass(): String {
        return this.funMember() + this.propertyMember
    }

    fun usageWithExtensionInsideClass(): String {
        return A("e ").funMember() + A("e ").propertyMember
    }

    fun usageWithContextAndExtensionInsideClass(): String {
        var temp = ""
        context(A("c ")) {
            temp = A("e ").<!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>funMember<!>() + A("e ").<!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>propertyMember<!>
        }
        return temp
    }
}

fun simpleUsageOutsideClass(): String {
    with(A("d ")) {
        return funMember() + propertyMember
    }
}

fun usageWithExtensionOutsideClass(): String {
    with(A("d ")) {
        return A("e ").funMember() + A("e ").propertyMember
    }
}

fun usageWithExtensionAndContextOutsideClass(): String {
    var temp = ""
    with(A("d ")) {
        context(A("c ")) {
            temp =  A("e ").<!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>funMember<!>() + A("e ").<!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>propertyMember<!>
        }
    }
    return temp
}

fun box(): Boolean {
    return (
        (A().simpleUsageInsideClass() == "d d d d d d ") &&
        (A().usageWithThisInsideClass() == "d d d d d d ") &&
        (A().usageWithExtensionInsideClass() == "d d e d d e ") &&
        (A().usageWithContextAndExtensionInsideClass() == "c d e c d e ") &&
        (simpleUsageOutsideClass() == "d d d d d d ") &&
        (usageWithExtensionOutsideClass() == "d d e d d e ") &&
        (usageWithExtensionAndContextOutsideClass() == "c d e c d e ")
    )
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, classDeclaration, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, functionalType, getter, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, stringLiteral, thisExpression, typeParameter, typeWithContext */
