// !WITH_NEW_INFERENCE
// FILE: b.kt
package outer

fun Int?.optint() : Unit {}
val Int?.optval : Unit get() = Unit

fun <T: Any, E> T.foo(<!UNUSED_PARAMETER!>x<!> : E, y : A) : T   {
  y.plus(1)
  y plus 1
  y + 1.0

  this<!UNNECESSARY_SAFE_CALL!>?.<!>minus<T>(this)

  return this
}

class A

infix operator fun A.plus(<!UNUSED_PARAMETER!>a<!> : Any) {

  1.foo()
  true.<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>)<!>

  <!UNUSED_EXPRESSION!>1<!>
}

operator fun A.plus(<!UNUSED_PARAMETER!>a<!> : Int) {
  <!UNUSED_EXPRESSION!>1<!>
}

operator fun <T> T.minus(<!UNUSED_PARAMETER!>t<!> : T) : Int = 1

fun test() {
  val <!UNUSED_VARIABLE!>y<!> = 1.abs
}
val Int.abs : Int
  get() = if (this > 0) this else -this;

<!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>val <T> T.foo : T<!>

fun Int.foo() = this

// FILE: b.kt
package null_safety

import outer.*

        fun parse(<!UNUSED_PARAMETER!>cmd<!>: String): Command? { return null  }
        class Command() {
        //  fun equals(other : Any?) : Boolean
          val foo : Int = 0
        }

        fun Any.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(<!UNUSED_PARAMETER!>other<!> : Any?) : Boolean = true
        fun Any?.equals1(<!UNUSED_PARAMETER!>other<!> : Any?) : Boolean = true
        fun Any.equals2(<!UNUSED_PARAMETER!>other<!> : Any?) : Boolean = true

        fun main() {

            System.out.print(1)

            val command = parse("")

            command.foo

            command<!UNSAFE_CALL!>.<!>equals(null)
            command?.equals(null)
            command.equals1(null)
            command?.equals1(null)

            val c = Command()
            c<!UNNECESSARY_SAFE_CALL!>?.<!>equals2(null)

            if (command == null) <!UNUSED_EXPRESSION!>1<!>
        }
