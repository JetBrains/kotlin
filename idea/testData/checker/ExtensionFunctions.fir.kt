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
  true.<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /foo">foo</error>()

  1
}

infix operator fun A.plus(a : Int) {
  1
}

fun <T> T.minus(t : T) : Int = 1

fun test() {
  val y = 1.abs
}
val Int.abs : Int
  get() = if (this > 0) this else -this;

<error descr="[EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT] Extension property must have accessors or be abstract">val <T> T.foo : T</error>

fun Int.foo() = this

// FILE: b.kt
//package null_safety

        fun parse(cmd: String): Command? { return null  }
        class Command() {
        //  fun equals(other : Any?) : Boolean
          val foo : Int = 0
        }

        operator fun Any.equals(other : Any?) : Boolean = true
        fun Any?.equals1(other : Any?) : Boolean = true
        fun Any.equals2(other : Any?) : Boolean = true

        fun main(args: Array<String>) {

            System.out.print(1)

            val command = parse("")

            command.foo

            command<error descr="[UNSAFE_CALL] Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Command?">.</error>equals(null)
            command?.equals(null)
            command.equals1(null)
            command?.equals1(null)

            val c = Command()
            c?.equals2(null)

            if (command == null) 1
        }
