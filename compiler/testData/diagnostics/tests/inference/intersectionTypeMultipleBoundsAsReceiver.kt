// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Foo<T>
interface Bar<T>

class Baz<T> : Foo<T>, Bar<T>

fun <T, S> S.bip(): String where S : Foo<T>, S: Bar<T> {
    return "OK"
}

fun box(): String {
    val baz = Baz<String>()
    return baz.bip()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
localProperty, nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
