// IGNORE_FIR_METADATA_LOADING_K2
//   Ignore reason: KT-58080

@file:JvmMultifileClass
@file:JvmName("Test")
package test

annotation class Anno(val value: String)

@Anno(constant)
const val constant = "OK"
