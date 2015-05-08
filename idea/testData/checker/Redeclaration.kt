val <error>a</error> : Int = 1
val <error>a</error> : Int = 1

<error>fun foo()</error> {}
<error>fun foo()</error> {}

enum class EnumClass {
    <error>FOO</error>,
    <error>FOO</error>
}

class A {
    val <error>a</error> : Int = 1
    val <error>a</error> : Int = 1

    <error>fun foo()</error> {}
    <error>fun foo()</error> {}
}

object B {
    class <error>C</error>
    class <error>C</error>
}

fun PairParam<<error>T</error>, <error>T</error>>() {}
class PParam<<error>T</error>, <error>T</error>> {}

val <<error>T</error>, <error>T</error>> T.fooParam : Int get() = 1
