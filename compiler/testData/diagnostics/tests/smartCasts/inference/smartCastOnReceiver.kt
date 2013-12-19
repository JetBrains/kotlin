trait A
trait B

class Test {
    fun test(a: A?, b: B, list: MutableList<Pair<A, B>>) {
        if (a != null) {
            list.add(<!DEBUG_INFO_AUTOCAST!>a<!> to b)
        }
    }
}

class Pair<out A, out B>(val first: A, val second: B)
fun <A, B> A.to(that: B) = Pair(this, that)