// WITH_RUNTIME

class A(val x: String)

fun foo(a: A) {
    a.x.substring<caret>(0, a.x.indexOf('x'))
}