// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-42169

open class Foo<T>
class Foo1<A> : Foo<Int>()
class Foo2 : Foo<Int>()

fun process(foo: Foo<Long>) {
    foo as Foo1<*>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType, starProjection, typeParameter */
