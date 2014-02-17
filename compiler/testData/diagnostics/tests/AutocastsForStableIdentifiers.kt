// FILE: a.kt
package example.ns
val y : Any? = 2

// FILE: b.kt
package example

object Obj {
    val y : Any? = 2
}

class AClass() {
    class object {
        val y : Any? = 2
    }
}

val x : Any? = 1

fun Any?.vars(<!UNUSED_PARAMETER!>a<!>: Any?) : Int {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>b<!>: Int = 0
    if (ns.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>ns.y<!>
    }
    if (ns.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>example.ns.y<!>
    }
    if (example.ns.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>ns.y<!>
    }
    if (example.ns.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>example.ns.y<!>
    }
//    if (package.bottles.ns.y is Int) {
//        b = ns.y
//    }
    if (Obj.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>Obj.y<!>
    }
    if (example.Obj.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>Obj.y<!>
    }
    if (AClass.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>AClass.y<!>
    }
    if (example.AClass.y is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>AClass.y<!>
    }
    if (x is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>x<!>
    }
    if (example.x is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>x<!>
    }
    if (example.x is Int) {
        b = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>example.x<!>
    }
    return 1
}

fun Any?.foo() : Int {
    if (this is Int)
      return <!DEBUG_INFO_AUTOCAST!>this<!>
    if (this@foo is Int)
      return <!DEBUG_INFO_AUTOCAST!>this<!>
    if (this@foo is Int)
      return <!DEBUG_INFO_AUTOCAST!>this@foo<!>
    if (this is Int)
      return <!DEBUG_INFO_AUTOCAST!>this@foo<!>
    return 1
}


trait T {}

open class C {
    fun foo() {
       var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>t<!> : T? = null
       if (this is T) {
          t = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>this<!>
       }
       if (this is T) {
          t = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>this@C<!>
       }
       if (this@C is T) {
          t = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>this<!>
       }
       if (this@C is T) {
          t = <!UNUSED_VALUE, DEBUG_INFO_AUTOCAST!>this@C<!>
       }
    }
}