// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-40687

// KT-40687: False-positive TYPE_MISMATCH: KClass<Captured(*)> is not a subtype of KClass<Any>
import kotlin.reflect.KClass

fun foo(locationClass: KClass<*>?) {
    val type = locationClass ?: Any::class
}

/* GENERATED_FIR_TAGS: classReference, elvisExpression, functionDeclaration, localProperty, nullableType, outProjection,
propertyDeclaration, starProjection */
