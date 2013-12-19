package test

val Long.date1: Object = java.util.Date()

internal val Long.date12: Object = java.util.Date()

private val Long.date3: java.util.Date = java.util.Date()

public val Long.date4: java.util.Date = java.util.Date()