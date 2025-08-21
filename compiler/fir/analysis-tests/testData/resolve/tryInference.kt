// RUN_PIPELINE_TILL: FRONTEND
fun <T> materialize(): T = throw Exception()

interface A

fun takeA(a: A) {}

fun test() {
    takeA(
        try {
            materialize()
        } catch (e: Exception) {
            materialize()
        } finally {
            <!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>() // Should be an errror
        }
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, localProperty, nullableType, propertyDeclaration,
tryExpression, typeParameter */
