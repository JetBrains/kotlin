// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88675, KT-88681
// WITH_STDLIB
// FULL_JDK
// RENDER_DIAGNOSTIC_ARGUMENTS

// Variations of the reporter's case collected in the comments of KT-88675: all of them used to crash
// the same way as the original one

interface Artifact
class MyArtifact(val value: String) : Artifact

val strings: List<String> = listOf()
val c = true

fun <X> pick(a: X, b: X): X = a

object DelegateExpressions {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val whenExpression by when {
        c -> strings.map { MyArtifact(it) }
        else -> emptyList()
    }
    val probeWhen: List<MyArtifact> = whenExpression

    val tryExpression by try {
        strings.map { MyArtifact(it) }
    } catch (e: Exception) {
        emptyList()
    }
    val probeTry: List<MyArtifact> = tryExpression

    val elvis by strings.takeIf { c }?.map { MyArtifact(it) } ?: emptyList()
    val probeElvis: List<MyArtifact> = elvis

    val insideRun by run { if (c) strings.map { MyArtifact(it) } else emptyList() }
    val probeRun: List<MyArtifact> = insideRun

    // No `if` at all: a generic call with two arguments
    val picked by pick(strings.map { MyArtifact(it) }, emptyList())
    val probePicked: List<MyArtifact> = picked
}

object Branches {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    // `List<Artifact>`, the same as for `by buildList { add(MyArtifact("a")) }` alone
    val fromBuildList by if (c) buildList { add(MyArtifact("a")) } else emptyList()
    val probeBuildList: List<Artifact> = fromBuildList

    val fromListConstructor by if (c) List(1) { MyArtifact("a") } else emptyList()
    val probeListConstructor: List<MyArtifact> = fromListConstructor

    val withListOf by if (c) strings.map { MyArtifact(it) } else listOf()
    val probeListOf: List<MyArtifact> = withListOf

    val withMutableListOf by if (c) strings.map { MyArtifact(it) } else mutableListOf()
    val probeMutableListOf: List<MyArtifact> = withMutableListOf

    val withJavaEmptyList by if (c) strings.map { MyArtifact(it) } else java.util.Collections.emptyList()
    val probeJavaEmptyList: List<MyArtifact> = withJavaEmptyList

    val withEmptySet by if (c) strings.map { MyArtifact(it) } else emptySet()
    val probeEmptySet: Collection<MyArtifact> = withEmptySet
}

object SmallestTrigger {
    operator fun <L : Collection<Any>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val twoEmptyLists by if (c) emptyList() else emptyList()
    val probeTwoEmptyLists: List<Any> = twoEmptyLists

    val listOfIntAndEmpty by if (c) listOf(1) else emptyList()
    val probeListOfInt: List<Int> = listOfIntAndEmpty

    val emptyListAndSet by if (c) emptyList() else emptySet()
    val probeEmptyListAndSet: Collection<Any> = emptyListAndSet

    val explicitType: List<Any> by if (c) emptyList() else emptyList()
}

object BoundOnlyOnProvideDelegate {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object SingleParameterCollection {
    operator fun <L : Collection<Artifact>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object SingleParameterList {
    operator fun <L : List<Artifact>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object SingleParameterIterable {
    operator fun <L : Iterable<Artifact>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object WithKProperty {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: kotlin.reflect.KProperty<*>): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

// The reporter's form: member extensions of a base class, the property in a subclass
abstract class Base {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Base, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Base, property: Any): L = this
}

class MemberExtensionsOfBase : Base() {
    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

class Box<out V>(val v: V)
fun <V> box(f: () -> V): Box<V> = Box(f())

object BoxReceiver {
    operator fun <T : Artifact, B : Box<T>> B.provideDelegate(thisRef: Any?, property: Any): B = this
    operator fun <B> B.getValue(thisRef: Any?, property: Any): B = this

    val boxed by if (c) box { MyArtifact("a") } else box { MyArtifact("b") }
    val probe: Box<MyArtifact> = boxed
}

object MapReceiver {
    operator fun <T : Artifact, M : Map<String, T>> M.provideDelegate(thisRef: Any?, property: Any): M = this
    operator fun <M> M.getValue(thisRef: Any?, property: Any): M = this

    val mapped by if (c) strings.associateWith { MyArtifact(it) } else emptyMap()
    val probe: Map<String, MyArtifact> = mapped
}

object Contexts {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.setValue(thisRef: Any?, property: Any, value: L) {}

    class InClass {
        val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
        val probe: List<MyArtifact> = artifacts
    }

    object InObject {
        val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
        val probe: List<MyArtifact> = artifacts
    }

    fun local() {
        val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
        val probe: List<MyArtifact> = artifacts
    }

    val String.extensionProperty by if (c) strings.map { MyArtifact(it) } else emptyList()
    fun useExtensionProperty(s: String) {
        val probe: List<MyArtifact> = s.extensionProperty
    }

    val explicitType: List<MyArtifact> by if (c) strings.map { MyArtifact(it) } else emptyList()

    val insideBuildList = buildList {
        val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
        add(artifacts)
    }
    val probeInsideBuildList: List<List<MyArtifact>> = insideBuildList

    fun <A : Artifact> generic(src: List<A>) {
        val artifacts by if (c) src.map { it } else emptyList()
        val probe: List<A> = artifacts
    }

    fun fromRun() = run {
        val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
        artifacts
    }
    val probeFromRun: List<MyArtifact> = fromRun()

    var mutable by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probeMutable: List<MyArtifact> = mutable
}

// Started to crash in 2.5 only
object FinalClassBound {
    operator fun <T : MyArtifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : MyArtifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object ComparableBound {
    operator fun <T : Comparable<T>, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val lengths by if (c) strings.map { it.length } else emptyList()
    val probe: List<Int> = lengths
}

class Inv<V>
fun <V> inv(): Inv<V> = Inv()

object InvariantBound {
    operator fun <L : Inv<String>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val invariant by if (c) inv() else inv()
    val probe: Inv<String> = invariant
}

