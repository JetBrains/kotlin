// FIR_IGNORE
package a.b

class C<T, out S> {
    inner class D<R, in P> {

    }
}

interface Test {
//      C<out CharSequence, *>.D<in collections/List<*>, *>
//      │      class C<T, S>               collections/List<*>
//      │      │                           │
    val x: a.b.C<out CharSequence, *>.D<in List<*>, *>
}
