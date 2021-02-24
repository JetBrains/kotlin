// !WITH_NEW_INFERENCE
// FILE: a.kt
package outer

fun Int?.optint() : Unit {}
val Int?.optval : Unit get() = Unit

fun <T: Any, E> T.foo(x : E, y : A) : T   {
  y.plus(1)
  y plus 1
  y + 1.0

  this?.minus<T>(this)

  return this
}

class A

infix operator fun A.plus(a : Any) {

  1.foo()
  true.<!INAPPLICABLE_CANDIDATE!>foo<!>()

  1
}

operator fun A.plus(a : Int) {
  1
}

operator fun <T> T.minus(t : T) : Int = 1

fun test() {
  val y = 1.abs
}
val Int.abs : Int
  get() = if (this > 0) this else -this;

val <T> T.foo : T

fun Int.foo() = this

// FILE: b.kt
package null_safety

import outer.*

        fun parse(cmd: String): Command? { return null  }
        class Command() {
        //  fun equals(other : Any?) : Boolean
          val foo : Int = 0
        }

        fun Any.equals(other : Any?) : Boolean = true
        fun Any?.equals1(other : Any?) : Boolean = true
        fun Any.equals2(other : Any?) : Boolean = true

        fun main() {

            System.out.print(1)

            val command = parse("")

            command.foo

            command<!UNSAFE_CALL!>.<!>equals(null)
            command?.equals(null)
            command.equals1(null)
            command?.equals1(null)

            val c = Command()
            c?.equals2(null)

            if (command == null) 1
        }
