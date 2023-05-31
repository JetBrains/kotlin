// TARGET_BACKEND: JVM
package test

val Long.date1: Any get() = java.util.Date()

internal val Long.date12: Any get() = java.util.Date()

private val Long.date3: java.util.Date get() = java.util.Date()

public val Long.date4: java.util.Date get() = java.util.Date()
