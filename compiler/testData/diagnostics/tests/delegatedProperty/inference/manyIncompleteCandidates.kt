// !WITH_NEW_INFERENCE
// FILE: main.kt
package test

import first.*
import second.*

val a12 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>A()<!>

// FILE: first.kt
package first

import kotlin.reflect.KProperty

class A

public operator fun <T> A.getValue(thisRef: Any?, property: KProperty<*>): T = null!!


// FILE: second.kt
package second

import first.A
import kotlin.reflect.KProperty

public operator fun <T> A.getValue(thisRef: Any?, property: KProperty<*>): T = null!!