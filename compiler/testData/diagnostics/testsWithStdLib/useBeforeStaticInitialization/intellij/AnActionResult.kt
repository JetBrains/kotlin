// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class AnActionResult {
    val isPerformed: Boolean
        get() = this is Performed

    val isIgnored: Boolean
        get() = this is Ignored

    val isFailed: Boolean
        get() = this is Failed

    <!POSSIBLE_INITIALIZATION_DEADLOCK!>class Performed : AnActionResult()<!>

    <!POSSIBLE_INITIALIZATION_DEADLOCK!>class Ignored(
        val reason: String,
    ) : AnActionResult()<!>

    class Failed(
        val cause: Throwable,
    ) : AnActionResult()

    companion object {
        @JvmField
        val IGNORED: AnActionResult = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>Ignored("unknown reason")<!>

        @JvmField
        val PERFORMED:AnActionResult = <!CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS!>Performed()<!>

        @JvmStatic
        fun failed(cause: Throwable): AnActionResult {
            return Failed(cause)
        }

        @JvmStatic
        fun ignored(reason: String): AnActionResult {
            return Ignored(reason)
        }
    }
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, isExpression, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, stringLiteral, thisExpression */
