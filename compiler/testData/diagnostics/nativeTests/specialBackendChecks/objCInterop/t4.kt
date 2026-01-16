// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

fun foo() = println(<!CALLABLE_REFERENCES_TO_VARIADIC_OBJECTIVE_C_METHODS_ARE_NOT_SUPPORTED!>NSAssertionHandler()::handleFailureInFunction<!>)
