// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-74227

interface Scope<out S : Element>
class WrappedScope(val scope: Scope<SomeElement>)

private fun bar(
    a: Scope<SomeElement>.() -> Unit,
    b: (WrappedScope.() -> Unit)?
) = foo(
    {
        a.invoke(this)
    }, b?.let {
    {
        WrappedScope(this)
    }
})

fun <F : Element> foo(
    a: (Scope<F>.() -> Unit)?,
    b: (Scope<F>.() -> Unit)?
) {
}

open class Element
class SomeElement : Element()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, out, primaryConstructor, propertyDeclaration, safeCall, thisExpression, typeConstraint, typeParameter,
typeWithExtension */
