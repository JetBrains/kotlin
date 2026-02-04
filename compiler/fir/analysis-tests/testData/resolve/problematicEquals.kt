// RUN_PIPELINE_TILL: BACKEND

data class A(val name: String)
data class B(val name: String)

fun test1(a: A, b: B): Boolean = <!PROBLEMATIC_EQUALS!>a == b<!>
fun test1b(a: A?, b: B?): Boolean = a == b

fun test2(a: Any?, b: B): Boolean = a == b
fun test2b(a: Any?, b: B?): Boolean = a == b

fun test3(a: A, b: Any?): Boolean = a == b
fun test3b(a: A?, b: Any?): Boolean = a == b

fun test4(a: Any?, b: Any?): Boolean {
    val x = a == b
    if (a is A) {
        val y = a == b
    }
    if (b is B) {
        val z = a == b
    }
    if (a is A && b is B) {
        val w = <!PROBLEMATIC_EQUALS!>a == b<!>
    }
    return true
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
isExpression, localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast */
