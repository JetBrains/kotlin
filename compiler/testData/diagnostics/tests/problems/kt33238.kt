// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-33238

// KT-33238: False positive USELESS_CAST in delegate init block with internal type value cast to public type

import kotlin.reflect.KProperty

class Delegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = TODO()
}

fun <T> delegate(init: () -> T): Delegate<T> = TODO()

class Bar {
    private val baz: Internal = Internal()
    val qux by delegate { baz as Open }
}

open class Open
internal class Internal : Open()

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, nullableType,
operator, propertyDeclaration, propertyDelegate, starProjection, typeParameter */
