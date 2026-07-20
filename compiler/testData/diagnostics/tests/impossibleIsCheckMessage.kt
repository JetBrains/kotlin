// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87368
// RENDER_DIAGNOSTICS_FULL_TEXT

class KotlinType
data class LocalClass(val type: KotlinType)
interface MyInterface

fun testIntersection(value: Any) {
    if (value is LocalClass) {
        if (<!IMPOSSIBLE_IS_CHECK_WARNING!>value is MyInterface<!>) {
            val type: KotlinType = value.type
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, localProperty, primaryConstructor, propertyDeclaration, smartcast */
