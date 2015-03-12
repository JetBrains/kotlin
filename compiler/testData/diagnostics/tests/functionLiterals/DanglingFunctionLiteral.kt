
class Foo() {
    private val builder = StringBuilder("sdfsd")

    init {
        }
}

class Foo1() {
    private val builder = <!NONE_APPLICABLE!>StringBuilder<!>("sdfsd")

        <!DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{
        }<!>
}

fun foo() = {
  <!NONE_APPLICABLE!>println<!>(1)
  <!DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>
}

fun foo1() = {
  println(1);
  {}
}

fun println(<!UNUSED_PARAMETER!>i<!> : Int) {}
fun println(<!UNUSED_PARAMETER!>s<!> : Byte) {}
fun println() {}
