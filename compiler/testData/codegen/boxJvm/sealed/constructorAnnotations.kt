// TARGET_BACKEND: JVM
// WITH_REFLECT

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