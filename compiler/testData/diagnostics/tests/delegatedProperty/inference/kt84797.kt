// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +SyntheticTypeVariablesForCstOverTypeVariables
// FIR_DUMP
// ISSUE: KT-84797, KT-88675
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

interface Artifact
class MyArtifact(val value: String) : Artifact
class OtherArtifact : Artifact

object WithBound {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    class Foo {
        val strings: List<String> = listOf()
        val others: List<OtherArtifact> = listOf()

        // One branch is proper, the other one has a not-fixed element variable
        val artifacts by if (true) others else strings.map { MyArtifact(it) }
        val probeArtifacts: List<Artifact> = artifacts

        val artifactsReversed by if (true) strings.map { MyArtifact(it) } else others
        val probeReversed: List<Artifact> = artifactsReversed
    }
}

object WithoutBound {
    operator fun <T, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    class Foo {
        val strings: List<String> = listOf()
        val ints: List<Int> = listOf()

        val mixed by if (true) ints else strings.map { it.length.toString() }
        val probeMixed: List<Any> = mixed
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, lambdaLiteral, nestedClass, nullableType, objectDeclaration, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, thisExpression, typeConstraint, typeParameter */
