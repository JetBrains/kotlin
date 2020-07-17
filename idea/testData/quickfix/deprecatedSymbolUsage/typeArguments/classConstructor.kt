// "Replace with 'A<TElement>({t})'" "true"

open class A<T> constructor(t: () -> T) {
    @Deprecated("F", ReplaceWith("A<T>({t})"))
    constructor(t: T) : this({ t })
}

class B<TElement>(t: TElement) : A<caret><TElement>(t)

fun b() {
    A<Int>(42)
}