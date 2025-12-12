// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

import kotlin.reflect.KClass

fun interface MyAction<F> {
    fun execute(f: F)
}

interface Inv<E> {}

fun <S : T, T : Any> Inv<T>.named(name: String, type: KClass<S>, configurationAction: MyAction<in S>): Int = 1
fun <T : Any> Inv<out Any>.named(name: String, type: KClass<T>, configuration: T.() -> Unit): String = ""

fun myUnit() {}

fun foo(inv: Inv<String>) {
    val x1 = inv.named("", String::class) { myUnit() }
    val x2 = inv.named("", Int::class) { myUnit() }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x2<!>
}

/* GENERATED_FIR_TAGS: classReference, funInterface, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, integerLiteral, interfaceDeclaration, lambdaLiteral, localProperty, nullableType, outProjection,
propertyDeclaration, stringLiteral, typeConstraint, typeParameter, typeWithExtension */
