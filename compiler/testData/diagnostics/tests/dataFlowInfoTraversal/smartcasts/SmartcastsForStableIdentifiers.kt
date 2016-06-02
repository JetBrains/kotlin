// FILE: a.kt
package example.ns

val y : Any? = 2

// FILE: b.kt
package example

import example.ns.y

object Obj {
    val y : Any? = 2
}

class AClass() {
    companion object {
        val y : Any? = 2
    }
}

val x : Any? = 1

fun Any?.vars(<!UNUSED_PARAMETER!>a<!>: Any?) : Int {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>b<!>: Int = 0
    if (example.ns.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>y<!>
    }
    if (example.ns.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>example.ns.y<!>
    }
    if (Obj.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>Obj.y<!>
    }
    if (example.Obj.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>Obj.y<!>
    }
    if (AClass.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>AClass.y<!>
    }
    if (example.AClass.y is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>AClass.y<!>
    }
    if (x is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>x<!>
    }
    if (example.x is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>x<!>
    }
    if (example.x is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>example.x<!>
    }
    return 1
}

fun Any?.foo() : Int {
    if (this is Int)
      return <!DEBUG_INFO_SMARTCAST!>this<!>
    if (this@foo is Int)
      return <!DEBUG_INFO_SMARTCAST!>this<!>
    if (this@foo is Int)
      return <!DEBUG_INFO_SMARTCAST!>this@foo<!>
    if (this is Int)
      return <!DEBUG_INFO_SMARTCAST!>this@foo<!>
    return 1
}


interface T {}

open class C {
    fun foo() {
       var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>t<!> : T? = null
       if (this is T) {
          <!UNUSED_VALUE!>t =<!> <!DEBUG_INFO_SMARTCAST!>this<!>
       }
       if (this is T) {
          <!UNUSED_VALUE!>t =<!> <!DEBUG_INFO_SMARTCAST!>this@C<!>
       }
       if (this@C is T) {
          <!UNUSED_VALUE!>t =<!> <!DEBUG_INFO_SMARTCAST!>this<!>
       }
       if (this@C is T) {
          <!UNUSED_VALUE!>t =<!> <!DEBUG_INFO_SMARTCAST!>this@C<!>
       }
    }
}
