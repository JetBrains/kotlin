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
        b = <!UNUSED_VALUE!>ns.y<!>
    }
    if (ns.y is Int) {
        b = <!UNUSED_VALUE!>example.ns.y<!>
    }
    if (example.ns.y is Int) {
        b = <!UNUSED_VALUE!>ns.y<!>
    }
    if (example.ns.y is Int) {
        b = <!UNUSED_VALUE!>example.ns.y<!>
    }
//    if (namespace.bottles.ns.y is Int) {
//        b = ns.y
//    }
    if (Obj.y is Int) {
        b = <!UNUSED_VALUE!>Obj.y<!>
    }
    if (example.Obj.y is Int) {
        b = <!UNUSED_VALUE!>Obj.y<!>
    }
    if (AClass.y is Int) {
        b = <!UNUSED_VALUE!>AClass.y<!>
    }
    if (example.AClass.y is Int) {
        b = <!UNUSED_VALUE!>AClass.y<!>
    }
    if (x is Int) {
        b = <!UNUSED_VALUE!>x<!>
    }
    if (example.x is Int) {
        b = <!UNUSED_VALUE!>x<!>
    }
    if (example.x is Int) {
        b = <!UNUSED_VALUE!>example.x<!>
    }
    return 1
}

fun Any?.foo() : Int {
    if (this is Int)
      return this
    if (this@foo is Int)
      return this
    if (this@foo is Int)
      return this@foo
    if (this is Int)
      return this@foo
    return 1
}


trait T {}

open class C {
    fun foo() {
       var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>t<!> : T? = null
       if (this is T) {
          t = <!UNUSED_VALUE!>this<!>
       }
       if (this is T) {
          t = <!UNUSED_VALUE!>this@C<!>
       }
       if (this@C is T) {
          t = <!UNUSED_VALUE!>this<!>
       }
       if (this@C is T) {
          t = <!UNUSED_VALUE!>this@C<!>
       }
    }
}
