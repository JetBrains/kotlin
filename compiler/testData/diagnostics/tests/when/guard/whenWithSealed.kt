// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

infix fun Any.sealed(a: Any?) {}

fun WhenWithSealed(x: Any) {
    val x = 1 sealed when (x) {
        is BooleanHolder if x.value -> 1
        is BooleanHolder if !x.value -> 2
        else -> 3
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, guardCondition,
infix, integerLiteral, isExpression, localProperty, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
