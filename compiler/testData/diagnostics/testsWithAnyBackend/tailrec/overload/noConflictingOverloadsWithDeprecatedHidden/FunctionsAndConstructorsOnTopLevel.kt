// RUN_PIPELINE_TILL: BACKEND

class TestTailrecFunctionVsConstructor {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) constructor()
}
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun TestTailrecFunctionVsConstructor() {}

class TestTailrecFunctionVsConstructorReverse {
    constructor()
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun TestTailrecFunctionVsConstructorReverse() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, secondaryConstructor, stringLiteral, tailrec */
