@file:JvmName("TopLevelMultifile")
@file:JvmMultifileClass
package test

import kotlin.jvm.JvmName
import kotlin.jvm.JvmMultifileClass

<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): kotlin.Int
    fun getX(): kotlin.Int">val x</error> = 1
<error descr="[CONFLICTING_JVM_DECLARATIONS] Platform declaration clash: The following declarations have the same JVM signature (getX()I):
    fun <get-x>(): kotlin.Int
    fun getX(): kotlin.Int">fun getX()</error> = 1