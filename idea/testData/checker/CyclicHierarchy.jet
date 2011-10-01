trait A {
    fun foo() {}
}
trait B : A, <error>E</error> {}
trait C : B {}
trait D : <error>B</error> {}
trait E : <error>F</error> {}
trait F : <error>D</error>, C {}
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
    c?.foo()
    d?.foo()
    e?.<error>foo</error>()
    f?.foo()
    g?.foo()
    h?.foo()
}