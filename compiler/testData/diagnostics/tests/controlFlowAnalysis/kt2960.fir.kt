//KT-2960 Perform control flow checks for package property initializers

package b

class P {
    var x : Int = 0
        private set
}

val p = P()
var f = { -> p.x = 32 }

val o = object {
    fun run() {
        p.x = 4

        val z : Int
        doSmth(<!UNINITIALIZED_VARIABLE!>z<!>)
    }
}

val g = { ->
    val x: Int
    doSmth(<!UNINITIALIZED_VARIABLE!>x<!>)
}

class A {
    val a : Int = 1
      get() {
          val x : Int
          doSmth(<!UNINITIALIZED_VARIABLE!>x<!>)
          return field
      }
}

fun doSmth(i: Int) = i