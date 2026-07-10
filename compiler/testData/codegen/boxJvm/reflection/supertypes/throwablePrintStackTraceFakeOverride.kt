// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that Throwable, Exception, and Error expose printStackTrace(PrintStream)
// as a properly-typed member function via reflection.

import kotlin.reflect.full.*
import kotlin.test.*

fun checkPrintStackTrace(klass: kotlin.reflect.KClass<*>, label: String) {
    val fns = klass.memberFunctions.filter { it.name == "printStackTrace" }

    // Must have at least one printStackTrace overload
    assertTrue(fns.isNotEmpty(),
        "$label must have at least one printStackTrace() in memberFunctions")

    // Must have a no-arg overload
    assertTrue(fns.any { it.valueParameters.isEmpty() },
        "$label must have printStackTrace() with no parameters")

    // Must have a PrintStream overload
    val withParam = fns.firstOrNull { it.valueParameters.size == 1 }
    assertNotNull(withParam,
        "$label must have printStackTrace(PrintStream) overload, found: ${fns.map { it.parameters.map { p -> p.type } }}")

    // The PrintStream overload's parameter must be typed as PrintStream, not generic Any
    val paramType = withParam.valueParameters.single().type.toString()
    assertTrue(paramType.contains("PrintStream"),
        "$label.printStackTrace(PrintStream) parameter type must contain 'PrintStream', got: $paramType")
}

fun box(): String {
    checkPrintStackTrace(Throwable::class,          "Throwable")
    checkPrintStackTrace(Exception::class,           "Exception")
    checkPrintStackTrace(java.lang.Error::class,    "Error")
    checkPrintStackTrace(RuntimeException::class,   "RuntimeException")
    return "OK"
}
