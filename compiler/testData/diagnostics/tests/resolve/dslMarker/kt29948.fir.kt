// RUN_PIPELINE_TILL: BACKEND
@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class MyDsl<!>

@MyDsl
interface Foo<T> {
    val x: Int
}

val Foo<*>.bad: Int get() = x

fun Foo<*>.badFun(): Int = x

val Foo<Int>.good: Int get() = x

fun test(foo: Foo<*>) {
    foo.apply {
        x
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, starProjection,
typeParameter */
