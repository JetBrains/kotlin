// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67912
// WITH_STDLIB

interface Bound

inline fun <reified F : Bound> foo(key: String): F? = null

fun main() {
    val otherValue: Map<String, String> by lazy {
        <!TYPE_INTERSECTION_AS_REIFIED_ERROR!>foo<!>("") ?: emptyMap()
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, inline, interfaceDeclaration, intersectionType,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyDelegate, reified, stringLiteral,
typeConstraint, typeParameter */
