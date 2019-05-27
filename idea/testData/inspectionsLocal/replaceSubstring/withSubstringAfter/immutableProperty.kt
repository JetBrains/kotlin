// WITH_RUNTIME

class A(val x: String)

fun foo(a: A) {
    a.x.substring<caret>(a.x.indexOf('x'))
}