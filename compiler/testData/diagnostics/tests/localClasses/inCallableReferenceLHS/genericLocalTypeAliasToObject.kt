// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +LocalTypeAliases
object Foo

fun foo() {
    typealias TA<T> = Foo

    val x: () -> String = TA::toString
    val y = TA::toString
    val z = TA<*>::toString
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, localProperty, nullableType,
objectDeclaration, propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
