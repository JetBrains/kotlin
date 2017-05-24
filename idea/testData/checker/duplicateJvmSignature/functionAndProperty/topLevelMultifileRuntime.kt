// RUNTIME
@file:JvmName("TopLevelMultifile")
@file:JvmMultifileClass
package test

<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): Int defined in test
    fun getX(): Int defined in test">val x</error> = 1
<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): Int defined in test
    fun getX(): Int defined in test">fun getX()</error> = 1
