// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases
// DIAGNOSTICS: -REDUNDANT_RETURN

interface Base {
    fun implicitFunReturnType() = 1
    fun explicitFunReturnType() : Int = 1

    val implicitPropertyReturnType
        get() = 1

    val explicitPropertyReturnType: Int
        get() = 1
}

class Derived : Base {
    override fun implicitFunReturnType() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_TYPE_MISMATCH!>1<!>
    override fun explicitFunReturnType() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_TYPE_MISMATCH!>1<!>

    override val implicitPropertyReturnType
        get() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_TYPE_MISMATCH!>1<!>
    override val explicitPropertyReturnType
        get() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_TYPE_MISMATCH!>1<!>
}

class Derived2 : Base {
    override fun implicitFunReturnType(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1
    override fun explicitFunReturnType(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1

    override val implicitPropertyReturnType: Int
        get() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1
    override val explicitPropertyReturnType: Int
        get() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, interfaceDeclaration, override,
propertyDeclaration */
