// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88675, KT-91962
// WITH_STDLIB

interface Artifact
class MyArtifact(val value: String) : Artifact

val strings: List<String> = listOf()
val c = true

operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

class Variations {
    val byIf by if (c) strings.map { MyArtifact(it) } else emptyList()
    val byWhen by when {
        c -> strings.map { MyArtifact(it) }
        else -> emptyList()
    }
    val byTry by try {
        strings.map { MyArtifact(it) }
    } catch (e: Exception) {
        emptyList()
    }
    val byElvis by strings.takeIf { c }?.map { MyArtifact(it) } ?: emptyList()
    val byRun by run {
        if (c) strings.map { MyArtifact(it) } else emptyList()
    }
}

fun <X> pick(a: X, b: X): X = a

class GenericCall {
    val byPick by pick(strings.map { MyArtifact(it) }, emptyList())
}

class Branches {
    val byTwoEmptyLists by if (c) emptyList() else emptyList()
    val byListAndEmpty by if (c) listOf(MyArtifact("a")) else emptyList()
    val bySetAndEmpty by if (c) setOf(MyArtifact("a")) else emptySet()
}

object AnyBound {
    operator fun <T : Any, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Any, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val byAnyBound by if (c) strings.map { MyArtifact(it) } else emptyList()
}

class Box<out V>(val v: V)

fun <V> box(f: () -> V): Box<V> = Box(f())

object BoxDelegate {
    operator fun <T : Artifact, B : Box<T>> B.provideDelegate(thisRef: Any?, property: Any): B = this
    operator fun <B> B.getValue(thisRef: Any?, property: Any): B = this

    val byBox by if (c) box { MyArtifact("a") } else box { MyArtifact("b") }
}
