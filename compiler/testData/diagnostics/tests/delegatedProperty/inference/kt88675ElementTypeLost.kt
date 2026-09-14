// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +SyntheticTypeVariablesForCstOverTypeVariables
// ISSUE: KT-88675, KT-88681
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

// Variations from the comments of KT-88675 that did not crash, but lost the element type (`List<*>`)
// or reported a false error

interface Artifact
class MyArtifact(val value: String) : Artifact

val strings: List<String> = listOf()
val c = true

object NullableAnyBound {
    operator fun <T : Any?, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Any?, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object StarBound {
    operator fun <L : Collection<*>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

object NoBound {
    operator fun <L> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <L> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts

    val lengths by if (c) listOf("x").map { it.length } else emptyList()
    val probeLength: Int = lengths.first()
}

object BoundOnlyOnGetValue {
    operator fun <T, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    val artifacts by if (c) strings.map { MyArtifact(it) } else emptyList()
    val probe: List<MyArtifact> = artifacts
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression,
interfaceDeclaration, lambdaLiteral, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
propertyDelegate, starProjection, stringLiteral, thisExpression, typeConstraint, typeParameter */
