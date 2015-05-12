interface A {
    fun foo() {}
}
interface B : A, <error>E</error> {}
interface C : <error>B</error> {}
interface D : <error>B</error> {}
interface E : <error>F</error> {}
interface F : <error>D</error>, <error>C</error> {}
interface G : F {}
interface H : F {}

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