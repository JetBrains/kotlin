// "Replace usages of 'constructor A<T>(T, T = ...)' in whole project" "true"
// ERROR: Unresolved reference: T

open class A<T> constructor(t: () -> T, f: () -> T = t) {
    @Deprecated("F", ReplaceWith("A<T>({t})"))
    constructor(t: T, f: T = t) : this({ t })
}

class B<TElement>(t: TElement) : A<caret><TElement>(t)

fun b() {
    A<Int>(42)
}