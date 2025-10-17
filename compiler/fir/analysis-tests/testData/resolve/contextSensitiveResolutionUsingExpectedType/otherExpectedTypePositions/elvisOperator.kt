// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MyClass {
    object NestedInheritor : MyClass()

    companion object {
        val myClassProp: MyClass = MyClass()
        val stringProp: String = ""
        fun getNestedInheritor() = NestedInheritor
    }
}

fun <T>receive(e: T) {}
val ClassMemberAlias = MyClass.NestedInheritor

fun testElvis() {
    var i100: MyClass = NestedInheritor <!USELESS_ELVIS!>?: myClassProp<!>
    var i110: MyClass <!INITIALIZER_TYPE_MISMATCH!>=<!> NestedInheritor <!USELESS_ELVIS!>?: stringProp<!>
    var i120: MyClass = NestedInheritor <!USELESS_ELVIS!>?: <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()<!>

    receive<MyClass>(NestedInheritor <!USELESS_ELVIS!>?: myClassProp<!>)
    receive<MyClass>(NestedInheritor <!USELESS_ELVIS!>?: <!ARGUMENT_TYPE_MISMATCH!>stringProp<!><!>)
    receive<MyClass>(NestedInheritor <!USELESS_ELVIS!>?: <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, functionDeclaration, localProperty,
nestedClass, nullableType, objectDeclaration, propertyDeclaration, stringLiteral, typeParameter */
