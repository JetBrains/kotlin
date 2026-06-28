// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class BreakpointArea(open val line: Int) {

    abstract val isBetweenLines: Boolean


    <!POSSIBLE_INITIALIZATION_DEADLOCK!>data class OnLine(override val line: Int) : BreakpointArea(line) {
        override val isBetweenLines: Boolean get() = false
    }<!>

    companion object {
        @JvmField
        val INVALID: BreakpointArea = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>OnLine(-1)<!>
    }
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, getter, integerLiteral, nestedClass, objectDeclaration,
override, primaryConstructor, propertyDeclaration, sealed */
