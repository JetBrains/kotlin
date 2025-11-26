// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68606
interface Foo<E>

data class SerializerAndValue<T>(
    val serializer: Foo<T>,
    val value: T
)

fun <T> encodeToString(serializer: Foo<T>, value: T) {}

val a: (SerializerAndValue<*>) -> Unit = { (serializer, value) ->
    encodeToString(serializer, <!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

val b: (SerializerAndValue<*>) -> Unit = { serializerAndValue ->
    encodeToString(serializerAndValue.serializer, <!ARGUMENT_TYPE_MISMATCH!>serializerAndValue.value<!>)
}

val c: SerializerAndValue<*>.() -> Unit = {
    encodeToString(serializer, value)
}

val d: SerializerAndValue<*>.() -> Unit = {
    encodeToString(this.serializer, <!ARGUMENT_TYPE_MISMATCH!>this.value<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, data, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, outProjection, primaryConstructor, propertyDeclaration, starProjection,
thisExpression, typeParameter, typeWithExtension */
