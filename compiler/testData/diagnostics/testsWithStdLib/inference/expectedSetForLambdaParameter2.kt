// FIR_IDENTICAL
// ISSUE: KT-68940

private val myMap = MyMap { ids ->
    val f = func(ids)
    ids.associateWith { p -> f.firstOrNull { true } }
}

class MyMap<K, V>(
    private val transformer: (Set<K>) -> Map<K, V?>,
)

fun func(ids: Set<A>): List<B> = emptyList()

class A
class B
