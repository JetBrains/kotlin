// RUN_PIPELINE_TILL: BACKEND
// WITH_REFLECT

inline fun <reified T : kotlin.Enum<T>> safeValueOf(type: String?): T? {
    return type?.let { java.lang.Enum.valueOf(T::class.java, type) }
}
enum class TestEnum

fun main() {
    val value: TestEnum? = safeValueOf("test")
}

/* GENERATED_FIR_TAGS: classReference, enumDeclaration, flexibleType, functionDeclaration, inline, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, reified, safeCall, smartcast, stringLiteral,
typeConstraint, typeParameter */
