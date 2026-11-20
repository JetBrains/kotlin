// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

class Outer {
    <!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
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
