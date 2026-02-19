// LANGUAGE: -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
package a

class MyClass {
    fun component1(i: Int) {}
}

class MyClass2 {}

<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.2
<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.3

fun test(mc1: MyClass, mc2: MyClass2) {
    val (<!NO_VALUE_FOR_PARAMETER, OPERATOR_MODIFIER_REQUIRED!>a<!>, b) = <!COMPONENT_FUNCTION_MISSING!>mc1<!>
    val (c) = <!COMPONENT_FUNCTION_MISSING!>mc2<!>

    //a,b,c are error types
    use(a, b, c)
}

fun use(vararg a: Any?) = a

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, funWithExtensionReceiver, functionDeclaration,
localProperty, nullableType, outProjection, propertyDeclaration, vararg */
