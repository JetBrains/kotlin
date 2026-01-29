// RUN_PIPELINE_TILL: FRONTEND

open class A<T>(val value: T) {
    val v: Any?
        field: T = value
}

open class B<U>: A<MutableList<U>>(mutableListOf())

class C: B<String>() {
    fun f() {
        v<!NO_GET_METHOD!>[0]<!>.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, integerLiteral, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
