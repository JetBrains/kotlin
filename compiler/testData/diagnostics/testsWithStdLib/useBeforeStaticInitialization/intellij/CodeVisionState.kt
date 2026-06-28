// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class CodeVisionState(val isReady: Boolean, val result: Any) {
    companion object{
        val READY_EMPTY: Ready = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>Ready(emptyList<Any>())<!>
    }
    <!POSSIBLE_INITIALIZATION_DEADLOCK!>class Ready(lenses: Any) : CodeVisionState(true, lenses)<!>
    object NotReady : CodeVisionState(false, emptyList<Any>())
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed */
