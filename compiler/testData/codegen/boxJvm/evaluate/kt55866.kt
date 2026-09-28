// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: -ForbidNonLiteralStringArgumentsForCompilerRequiredAnnotationParameters

// File facades with custom JVM name confuse AndroidTestGenerator.
// IGNORE_BACKEND: ANDROID

@file:JvmName(TAG)
package root

private const val TAG = "Tagged"

class ConstParamFiller

fun box(): String = "OK"
