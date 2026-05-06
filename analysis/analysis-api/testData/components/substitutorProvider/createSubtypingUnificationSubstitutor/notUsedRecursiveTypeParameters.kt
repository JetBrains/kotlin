interface Base

class Child: Base

interface Recursive<S: Recursive<S>>

fun <T: R, R: I, I: Recursive<T>> Base.foo(x: T, y: R, z: I): T = {
    th<caret_1_target>is
}

fun usage(xx: Child) {
    x<caret_1_base>x
}
