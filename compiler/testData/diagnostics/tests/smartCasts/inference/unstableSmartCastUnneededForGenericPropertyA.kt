// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-78595

class Box<V>(val value: V)

interface TypeA
interface TypeB
object Objekt: TypeA, TypeB

private val objektBox = Box(Objekt)
val <R> R.boxed: Box<R> get() = objektBox <!UNCHECKED_CAST!>as Box<R><!>

fun <T> unbox(arg: Box<T>): T = arg.value
fun consumeBoxTypeA(arg: Box<TypeA>) {}
fun consumeBoxOutAny(arg: Box<out Any>) {}

fun test(a: TypeA, b: TypeB) {
    if (a.boxed == b.boxed) {
        val leaked = unbox(a.boxed)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeA")!>leaked<!>

        consumeBoxTypeA(a.boxed)

        consumeBoxOutAny(a.boxed)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, equalityExpression, functionDeclaration, getter, ifExpression,
interfaceDeclaration, localProperty, nullableType, objectDeclaration, outProjection, primaryConstructor,
propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
