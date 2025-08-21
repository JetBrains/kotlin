// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
data class Foo(val name: String)

fun test(foo: Foo?) {
    val <!UNUSED_VARIABLE!>s<!>: String? = foo?.name?.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, safeCall */
