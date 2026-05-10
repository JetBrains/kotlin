// RUN_PIPELINE_TILL: BACKEND
package test

class Box<T>(val value: T)

typealias Alias<TT> = Box<TT>

fun box(): String {
    val x = Alias<_>("OK")
    return x.value
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
