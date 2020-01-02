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

fun Any?.vars(a: Any?) : Int {
    var b: Int = 0
    if (example.ns.y is Int) {
        b = y
    }
    if (example.ns.y is Int) {
        b = example.ns.y
    }
    if (Obj.y is Int) {
        b = Obj.y
    }
    if (example.Obj.y is Int) {
        b = Obj.y
    }
    if (AClass.y is Int) {
        b = AClass.y
    }
    if (example.AClass.y is Int) {
        b = AClass.y
    }
    if (x is Int) {
        b = x
    }
    if (example.x is Int) {
        b = x
    }
    if (example.x is Int) {
        b = example.x
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


interface T {}

open class C {
    fun foo() {
       var t : T? = null
       if (this is T) {
          t = this
       }
       if (this is T) {
          t = this@C
       }
       if (this@C is T) {
          t = this
       }
       if (this@C is T) {
          t = this@C
       }
    }
}
