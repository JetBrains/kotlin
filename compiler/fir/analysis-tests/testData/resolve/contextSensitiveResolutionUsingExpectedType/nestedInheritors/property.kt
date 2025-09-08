// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-76400

fun receiveMyClass(m: MyClass) {}

sealed class MyClass {
    object NestedInheritor : MyClass()

    companion object {
        val myClassProp: MyClass = NestedInheritor
        val stringProp: String = ""
    }
}

val ClassMemberAlias = MyClass.NestedInheritor

fun testIfElse(i: Int) {
    receiveMyClass(
        if (i == 0) NestedInheritor
        else if (i == 1) <!ARGUMENT_TYPE_MISMATCH!>myClassProp<!>
        else if (i == 2) <!ARGUMENT_TYPE_MISMATCH!>stringProp<!>
        else ClassMemberAlias
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, nestedClass, objectDeclaration, propertyDeclaration, stringLiteral */
