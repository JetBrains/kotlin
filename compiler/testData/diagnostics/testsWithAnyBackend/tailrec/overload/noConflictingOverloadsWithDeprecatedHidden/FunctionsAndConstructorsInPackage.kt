// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS

package pkg


class TestTailrecFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun TestTailrecFunctionVsConstructor() {}<!>

class TestTailrecFunctionVsConstructorReverse {
    constructor()
}
<!NO_TAIL_CALLS_FOUND_IN_IR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec fun TestTailrecFunctionVsConstructorReverse() {}<!>


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor, stringLiteral, tailrec */
