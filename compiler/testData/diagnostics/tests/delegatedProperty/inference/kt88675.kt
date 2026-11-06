// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +SyntheticTypeVariablesForCstOverTypeVariables
// FIR_DUMP
// ISSUE: KT-88675, KT-88681
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS

interface Artifact
class MyArtifact(val value: String) : Artifact

object WithBound {
    operator fun <T : Artifact, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Artifact, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    class Foo {
        val strings: List<String> = listOf()

        // The reporter's case
        val artifacts by if (true) strings.map { MyArtifact(it) } else emptyList()
        val probeArtifacts: List<MyArtifact> = artifacts

        // The smallest form: two unrelated type variables in the branches
        val twoEmpty by if (true) emptyList() else emptyList()

        // Both branches need inference
        val twoMaps by if (true) strings.map { MyArtifact(it) } else strings.map { MyArtifact(it) }
        val probeTwoMaps: List<MyArtifact> = twoMaps

        // Reporter's "fine" cases
        val mappedFoo = strings.map { MyArtifact(it) }
        val thisIsFine by if (true) mappedFoo else emptyList()
        val probeFine: List<MyArtifact> = thisIsFine

        val noIf by strings.map { MyArtifact(it) }
        val probeNoIf: List<MyArtifact> = noIf
    }
}

object WithAnyBound {
    operator fun <T : Any, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T : Any, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    class Foo {
        val strings: List<String> = listOf()
        val artifacts by if (true) strings.map { MyArtifact(it) } else emptyList()
        val probe: List<MyArtifact> = artifacts
    }
}

object WithoutBound {
    operator fun <T, L : Collection<T>> L.provideDelegate(thisRef: Any?, property: Any): L = this
    operator fun <T, L : Collection<T>> L.getValue(thisRef: Any?, property: Any): L = this

    class Foo {
        val strings: List<String> = listOf()
        // Used to compile, but with `List<*>` inferred instead of `List<MyArtifact>`
        val artifacts by if (true) strings.map { MyArtifact(it) } else emptyList()
        val probe: List<MyArtifact> = artifacts
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression,
interfaceDeclaration, lambdaLiteral, nestedClass, nullableType, objectDeclaration, operator, primaryConstructor,
propertyDeclaration, propertyDelegate, thisExpression, typeConstraint, typeParameter */
