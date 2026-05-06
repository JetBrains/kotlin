// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
// WITH_STDLIB

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Named function
context(<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>ctx: CoroutineContext<!>)
fun namedFunction() {}

val anonFun = context(<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>ctx: CoroutineContext<!>) fun() {}

fun lambdaWrapper() {
    val lambda1: <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!> = <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>
    val lambda2: <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(MyContext) () -> Unit<!> = <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>
    val lambda3: MyFunctionalType1 = <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>
    parameterType <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>
}

fun parameterType(block: <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!>) {}

fun returnType(): <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!> = <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>

fun type1(): (<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!>) -> Unit = { x -> }
fun type2(): (<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!>) -> Unit = { x: <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!> -> }

fun <T> withGeneric(block: context(T) () -> Unit) {}
fun testGenericSubstitution() {
    withGeneric<CoroutineContext> <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>{}<!>
}

fun <T : CoroutineContext> withBoundedGeneric1(block: context(T) () -> Unit) {}
fun <T> withBoundedGeneric2(block: context(T) () -> Unit) where T : CoroutineContext, T : CharSequence {}

typealias MyFunctionalType1 = <!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>context(CoroutineContext) () -> Unit<!>
typealias MyFunctionalType2 = context(MyContext) () -> Unit
typealias MyContext = CoroutineContext

context(<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>ctx: MyContext<!>) fun withTypeAlias() {}

context(<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>ctx: CoroutineContext?<!>)
fun withNullable() {}

context(<!COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED!>ctx: CoroutineContext<!>)
val propertyWithContext: Int get() = 42

open class Base<T> {
    context(t: T) open fun foo() = Unit
    open fun foo() = Unit
}

class Kt : Base<CoroutineContext>() {}

fun wrapper() {
    Base<CoroutineContext>().foo() // Not possible to detect :(
    Kt().foo() // Not possible to detect :(
}

context(ctx: EmptyCoroutineContext) fun emptyCoroutineContextAllowed() = Unit
context(ctx: Any) fun anyCoroutineContextAllowed() = Unit

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionDeclarationWithContext, functionalType, getter,
integerLiteral, lambdaLiteral, localFunction, localProperty, nullableType, propertyDeclaration,
propertyDeclarationWithContext, typeAliasDeclaration, typeParameter, typeWithContext */
