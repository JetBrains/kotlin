interface Base

class Child: Base

interface Recursive<T: Recursive<T>>

fun <T: R, R: I, I: Recursive<T>> Base.f<caret>oo(x: T, y: R, z: I): T = x

fun usage(x: Child) {
    <expr>x</expr>
}