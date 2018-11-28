package a.b

class C<T, out S> {
    inner class D<R, in P> {

    }
}

interface Test {
    val x: a.b.C<out CharSequence, *>.D<in List<*>, *>
}