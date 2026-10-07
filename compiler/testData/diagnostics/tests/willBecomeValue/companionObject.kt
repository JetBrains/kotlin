// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

class Outer {
    @WillBecomeValue
    companion object {
        override fun toString(): String = "Companion"
    }
}

@WillBecomeValue
object Standalone {
    override fun toString(): String = "Standalone"
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, override,
stringLiteral */
