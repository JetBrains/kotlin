// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +SyntheticTypeVariablesForCstOverTypeVariables
// ISSUE: KT-88675, KT-88681
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

// Neighbours of the reporter's case from the comments of KT-88675 that have always compiled

interface Artifact
class MyArtifact(val value: String) : Artifact

val strings: List<String> = listOf()
val c = true

object WithBound {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val listOfAndEmpty by if (c) listOf(MyArtifact("a")) else emptyList()
    val probeListOfAndEmpty: List<MyArtifact> = listOfAndEmpty

    val listOfAndListOf by if (c) listOf(MyArtifact("a")) else listOf()
    val probeListOfAndListOf: List<MyArtifact> = listOfAndListOf

    val setOfAndEmptySet by if (c) setOf(MyArtifact("a")) else emptySet()
    val probeSetOfAndEmptySet: Set<MyArtifact> = setOfAndEmptySet

    val toSetAndEmptySet by if (c) strings.map { MyArtifact(it) }.toSet() else emptySet()
    val probeToSetAndEmptySet: Set<MyArtifact> = toSetAndEmptySet

    val mapped = strings.map { MyArtifact(it) }
    val mapAndMapped by if (c) strings.map { MyArtifact(it) } else mapped
    val probeMapAndMapped: List<MyArtifact> = mapAndMapped

    // The workaround from the issue
    val explicitEmptyList by if (c) strings.map { MyArtifact(it) } else emptyList<MyArtifact>()
    val probeExplicitEmptyList: List<MyArtifact> = explicitEmptyList

    val explicitMap by if (c) strings.map<String, MyArtifact> { MyArtifact(it) } else emptyList()
    val probeExplicitMap: List<MyArtifact> = explicitMap

    val emptyAndExplicitEmpty by if (c) emptyList() else emptyList<MyArtifact>()
    val probeEmptyAndExplicitEmpty: List<MyArtifact> = emptyAndExplicitEmpty
}

object SmallestTrigger {
    operator fun <L : Collection<Any>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val single by emptyList()
    val probeSingle: List<Any> = single

    val explicitAnyAndEmpty by if (c) emptyList<Any>() else emptyList()
    val probeExplicitAnyAndEmpty: List<Any> = explicitAnyAndEmpty
}

class Box<out V>(val v: V)
fun <V> box(f: () -> V): Box<V> = Box(f())

object BoxReceiver {
    operator fun <T : Artifact, B : Box<T>> B.provideDelegate(thisRef: Any?, property: Any): B = this
    operator fun <B> B.getValue(thisRef: Any?, property: Any): B = this

    val single by box { MyArtifact("a") }
    val probeSingle: Box<MyArtifact> = single
}

class Inv<V>
fun <V> inv(): Inv<V> = Inv()

object InvariantBound {
    operator fun <L : Inv<String>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val single by inv()
    val probeSingle: Inv<String> = single
}

// provideDelegate whose return type is not a type variable
object CollectionReceiver {
    operator fun <T : Artifact> Collection<T>.provideDelegate(thisRef: Any?, property: Any): Collection<T> = this
    operator fun <T : Artifact> Collection<T>.getValue(thisRef: Any?, property: Any): Collection<T> = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: Collection<MyArtifact> = artifacts
}

object ListReturn {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): List<T> = toList()
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

class Holder<V>(val v: V) {
    operator fun getValue(thisRef: Any?, property: Any): V = v
}

object HolderReturn {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): Holder<L> = Holder(this)

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object UnboundedCollectionReceiver {
    operator fun <T> Collection<T>.provideDelegate(thisRef: Any?, property: Any): Collection<T> = this
    operator fun <T> Collection<T>.getValue(thisRef: Any?, property: Any): Collection<T> = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: Collection<MyArtifact> = artifacts
}

// No provideDelegate at all
object GetValueWithBound {
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object GetValueWithoutBound {
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
interfaceDeclaration, lambdaLiteral, nullableType, objectDeclaration, operator, out, primaryConstructor,
propertyDeclaration, propertyDelegate, stringLiteral, thisExpression, typeConstraint, typeParameter */
