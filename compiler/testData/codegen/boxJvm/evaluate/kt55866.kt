// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: -ForbidNonLiteralStringArgumentsForCompilerRequiredAnnotationParameters

@file:JvmName(TAG)
package root

private const val TAG = "Tagged"

class ConstParamFiller

fun box(): String = "OK"
