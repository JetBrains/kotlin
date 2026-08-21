// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78002

@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val s: String)

const val foo = "1"

fun f1() {
    @Anno(foo)
    context(_: @Anno(foo) String)
    fun <@Anno(foo) T> (@Anno(foo) String).foo(@Anno(foo) foo: String) : @Anno(foo) String {
        return this
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, const, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, localFunction, nullableType, primaryConstructor, propertyDeclaration, stringLiteral,
thisExpression, typeParameter */
