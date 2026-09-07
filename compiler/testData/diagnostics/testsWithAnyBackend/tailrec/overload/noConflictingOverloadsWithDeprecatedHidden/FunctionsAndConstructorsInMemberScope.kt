// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -MISPLACED_TYPE_PARAMETER_CONSTRAINTS


class MemberScope {


    class TestTailrecFunctionVsConstructor {
        @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
    }
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun TestTailrecFunctionVsConstructor() {}

    class TestTailrecFunctionVsConstructorReverse {
        constructor()
    }
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun TestTailrecFunctionVsConstructorReverse() {}


}


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, secondaryConstructor, stringLiteral, tailrec */
