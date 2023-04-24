// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:JvmName(<!EVALUATED("Tagged")!>TAG<!>)
package root

private const val TAG = <!EVALUATED("Tagged")!>"Tagged"<!>

class ConstParamFiller

fun box(): String = "OK"
