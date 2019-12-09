// FIR_COMPARISON

// RUNTIME
@file:JvmName("TopLevelMultifile")
@file:JvmMultifileClass
package test

<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): Int defined in test in file topLevelMultifileRuntime.kt
    fun getX(): Int defined in test in file topLevelMultifileRuntime.kt">val x</error> = 1
<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): Int defined in test in file topLevelMultifileRuntime.kt
    fun getX(): Int defined in test in file topLevelMultifileRuntime.kt">fun getX()</error> = 1
