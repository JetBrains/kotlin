package test

import kotlin.jvm.internal.KotlinDelegatedMethod

fun findClassOrFail(className: String): Class<*> =
        try {
            Class.forName(className)
        }
        catch (e: Exception) {
            throw AssertionError("Class $className not found")
        }

fun box(): String {
    val testPackage = findClassOrFail("test.TestPackage")
    val kotlinDelegatedMethod = findClassOrFail("kotlin.jvm.internal.KotlinDelegatedMethod") as Class<Annotation>

    assert(testPackage.declaredMethods.size() > 0) { "Class ${testPackage.name} has no declared methods" }

    for (method in testPackage.declaredMethods) {
        val ann = method.getAnnotation(kotlinDelegatedMethod) as? KotlinDelegatedMethod
        if (ann == null) {
            throw AssertionError("Method ${method.name} has no ${kotlinDelegatedMethod.simpleName} annotation.")
        }

        val implementationClassName = ann.implementationClassName
        val implementationClass = try {
            Class.forName(implementationClassName)
        }
        catch (e: Exception) {
            throw AssertionError("Implementation class $implementationClassName for method ${method.name} not found.")
        }

        val implementationMethod = try {
            implementationClass.getMethod(method.name, *method.parameterTypes)
        }
        catch (e: Exception) {
            throw AssertionError("Implementation class $implementationClassName for method ${method.name} has no corresponding implementation method.")
        }

        assert(implementationMethod.modifiers == method.modifiers) {
            "Implementation method for ${method.name} in $implementationClassName: " +
            "expected modifiers: ${method.modifiers}; actual modifiers: ${implementationMethod.modifiers}"
        }
    }

    return "OK"
}