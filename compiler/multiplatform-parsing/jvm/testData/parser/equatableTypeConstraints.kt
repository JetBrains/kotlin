fun <S, T> List<S>.indexOfEquatable(element: T): Int where S == T = 0

class Box<S, T> where S == T

val <S, T> S.equatable: T? where S == T
    get() = null
