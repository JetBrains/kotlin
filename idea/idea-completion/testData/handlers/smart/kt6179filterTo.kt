import java.util.ArrayList

fun foo(c: Collection<String>) {
    val list = ArrayList<String>()
    c.filterTo0(<caret>)
}

fun <T, C : MutableCollection<in T>> Iterable<T>.filterTo0(destination: C, predicate: (T) -> Boolean): C = destination

//ELEMENT: list