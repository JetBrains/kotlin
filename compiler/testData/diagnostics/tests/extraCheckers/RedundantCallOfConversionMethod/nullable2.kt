// RUN_PIPELINE_TILL: BACKEND
// IS_APPLICABLE: false
// WITH_STDLIB
data class Foo(val name: String)

fun nullable2(foo: Foo?) {
    val <!UNUSED_VARIABLE!>s<!>: String = foo?.name.toString()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, safeCall */
