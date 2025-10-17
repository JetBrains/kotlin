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

val ClassMemberAlias = MyClass.NestedInheritor

fun <T>receive(e: T) {}
fun <T> run(b: () -> T): T = b()

fun testIfElse(i: Int) {
    val s: MyClass =
        if (i == 0) {
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            NestedInheritor
        }
        else if (i == 1) {
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            myClassProp
        }
        else ClassMemberAlias

    val s2: MyClass <!INITIALIZER_TYPE_MISMATCH!>=<!>
        if (i == 2) {
            <!UNRESOLVED_REFERENCE!>stringProp<!>
            stringProp
        }
        else ClassMemberAlias

    receive<MyClass>(
        if (i == 0) {
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            // KT-76400
        }
        else if (i == 1) {
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            // KT-76400
        }
        else if (i == 2) {
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
            <!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()
        }
        else if (i == 3) {
            <!UNRESOLVED_REFERENCE!>stringProp<!>
            <!UNRESOLVED_REFERENCE!>stringProp<!>
            // KT-76400
        }
        else ClassMemberAlias
    )

    run<MyClass> {
        if (i == 0) {
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            <!UNRESOLVED_REFERENCE!>NestedInheritor<!>
            // KT-76400
        }
        else if (i == 1) {
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            <!UNRESOLVED_REFERENCE!>myClassProp<!>
            // KT-76400
        }
        else ClassMemberAlias
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, functionalType,
ifExpression, integerLiteral, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, stringLiteral, typeParameter */
