@file:JvmMultifileClass
@file:JvmName("Test")
package test

annotation class Anno(val value: String)

@Anno(constant)
const val constant = "OK"
