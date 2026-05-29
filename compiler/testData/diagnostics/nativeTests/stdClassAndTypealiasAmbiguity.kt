// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
import kotlin.*
import kotlin.jvm.*
import kotlin.native.concurrent.*
import kotlin.native.*

@<!DEPRECATION_ERROR!>SharedImmutable<!>
@ThreadLocal
val x = 42

@Throws(Exception::class)
fun test() {}
