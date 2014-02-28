trait A {
    fun foo() {}
}
trait B : A, <error>E</error> {}
trait C : <error>B</error> {}
trait D : <error>B</error> {}
trait E : <error>F</error> {}
trait F : <error>D</error>, <error>C</error> {}
trait G : F {}
trait H : F {}

val a : A? = null
val b : B? = null
val c : C? = null
val d : D? = null
val e : E? = null
val f : F? = null
val g : G? = null
val h : H? = null

fun test() {
    a?.foo()
    b?.foo()
    c?.<error>foo</error>()
    d?.<error>foo</error>()
    e?.<error>foo</error>()
    f?.<error>foo</error>()
    g?.<error>foo</error>()
    h?.<error>foo</error>()
}