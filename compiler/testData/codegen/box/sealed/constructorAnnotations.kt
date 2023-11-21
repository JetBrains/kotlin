// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

annotation class Ann

sealed class Test @Ann constructor(@Ann val x: String)

fun box(): String {
    val testCtor = Test::class.constructors.single()

    val testCtorAnnClasses = testCtor.annotations.map { it.annotationClass }
    if (testCtorAnnClasses != listOf(Ann::class)) {
        throw AssertionError("Annotations on constructor: $testCtorAnnClasses")
    }

    for (param in testCtor.parameters) {
        val paramAnnClasses = param.annotations.map { it.annotationClass }
        if (paramAnnClasses != listOf(Ann::class)) {
            throw AssertionError("Annotations on constructor parameter $param: $paramAnnClasses")
        }
    }

    return "OK"
}
