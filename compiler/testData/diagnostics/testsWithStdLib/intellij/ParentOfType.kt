// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE_FEATURE_TOGGLED: FixesForIntersectionTypesIn25
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
import kotlin.reflect.KClass

fun <T : Number> Any.parentOfTypes(vararg classes: KClass<out T>): T? {
    throw IllegalStateException()
}

val some = "123".parentOfTypes(Int::class, Double::class)
val someBangBang = "123".parentOfTypes(Int::class, Double::class)!!
val someFlexibleElvis = "123".parentOfTypes(Int::class, Double::class) ?: "123".parentOfTypes(Int::class, Double::class)

/* GENERATED_FIR_TAGS: classReference, funWithExtensionReceiver, functionDeclaration, intersectionType, nullableType,
propertyDeclaration, stringLiteral, typeConstraint, typeParameter, vararg */
