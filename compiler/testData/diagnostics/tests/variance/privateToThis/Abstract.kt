internal abstract class Test</*0*/ in I> {
    private/*private to this*/ final fun foo(): I {
        throw Exception()
    }

    private/*private to this*/ final val i: I get() = foo()
}
