// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface FeatureSelector {
    fun select(availableFeatures: Set<String>): Selection

    fun select(featureDeclaration: String): Boolean {
        return select(setOf(featureDeclaration)).selectedFeatures.isNotEmpty()
    }

    <!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class Selection(val selectedFeatures: Set<String>) {
        <!POSSIBLE_INITIALIZATION_DEADLOCK!>class Complete(selectedFeatures: Set<String>) : Selection(selectedFeatures)<!>

        open class Incomplete(selectedFeatures: Set<String>) : Selection(selectedFeatures) {
            open val details: String = "Incomplete selection, only these were selected: $selectedFeatures"
        }

        companion object {
            val NOTHING = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>Complete(emptySet())<!>
        }
    }<!>

    companion object {
        val NOTHING = object : FeatureSelector {
            override fun select(availableFeatures: Set<String>): Selection = Selection.NOTHING
        }

        val EVERYTHING = object : FeatureSelector {
            override fun select(availableFeatures: Set<String>): Selection =
                Selection.Complete(availableFeatures)
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, override, primaryConstructor, propertyDeclaration, sealed,
stringLiteral */
