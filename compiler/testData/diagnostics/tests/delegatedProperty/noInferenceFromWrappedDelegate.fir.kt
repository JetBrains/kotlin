// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// WITH_REFLECT

import kotlin.reflect.KProperty

// Definitions
class State<S>(var value: S)
<!NOTHING_TO_INLINE!>inline<!> operator fun <V> State<V>.getValue(thisRef: Any?, property: KProperty<*>): V = value
inline fun <M> remember(block: () -> M): M = block()

// list should have a type of List<Int>, not Any?
val list by remember { State(listOf(0)) }
val first = list.<!IMPLICIT_PROPERTY_TYPE_ON_INVOKE_LIKE_CALL!>first<!>()

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
starProjection, typeParameter */
