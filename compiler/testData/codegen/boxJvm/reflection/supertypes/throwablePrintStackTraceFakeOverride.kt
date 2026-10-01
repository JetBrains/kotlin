// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that mapped Trowable built-ins Throwable expose the correct
// printStackTrace as properly-typed member functions via reflection.

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

    // Must have exactly two single-parameter overloads: PrintStream! and PrintWriter!
    val withParam = fns.filter { it.valueParameters.size == 1 }
    val paramTypes = withParam.map { it.valueParameters.single().type.toString() }
    assertTrue(withParam.size == 2,
        "$label must have exactly two printStackTrace(...) overloads with one parameter, found: $paramTypes")

    assertTrue(paramTypes.any { it.contains("PrintStream") },
        "$label must have printStackTrace(PrintStream!) overload, found: $paramTypes")
    assertTrue(paramTypes.any { it.contains("PrintWriter") },
        "$label must have printStackTrace(PrintWriter!) overload, found: $paramTypes")
}

fun box(): String {
    checkPrintStackTrace(object : Throwable() {} ::class,          "object")
    checkPrintStackTrace(Throwable::class,          "Throwable")
    checkPrintStackTrace(Exception::class,           "Exception")
    checkPrintStackTrace(java.lang.Error::class,    "Error")
    checkPrintStackTrace(RuntimeException::class,   "RuntimeException")
    return "OK"
}
