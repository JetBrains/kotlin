package test

class FunctionTypes {
    public fun<IP, R, P1> Function1<IP, R>.compose(f: (P1) -> IP): (P1) -> R {
        return { p1: P1 -> this(f(p1)) }
    }

    public fun<IP, R, P1> ((IP) -> R).compose2(f: (P1) -> IP) : (P1) -> R {
        return { p1: P1 -> this(f(p1)) }
    }

    public fun <A> (A.(A) -> A).foo() {
    }

    public fun <A> (A.(A) -> A)?.bar() {
    }
}
