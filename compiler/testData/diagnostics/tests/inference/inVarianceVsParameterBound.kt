// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-47508

open class A
open class B : A()
class C : B()

class Foo<T : A>(val x: T)

fun bar(x: Foo<in C>) {
    val y: A = x.x
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, inProjection, localProperty,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
