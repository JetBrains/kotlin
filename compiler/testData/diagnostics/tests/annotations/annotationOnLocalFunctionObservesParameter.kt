// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78002

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class Anno(val s: String)

const val foo = "1"

fun f1() {
    @Anno(foo)
    context(_: @Anno(foo) String)
    fun <@Anno(foo) T> (@Anno(foo) String).foo(@Anno(foo) foo: String = @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNINITIALIZED_PARAMETER!>foo<!>) "") : @Anno(foo) String {
        return this
    }

    class A {
        constructor(foo: @Anno(foo) String) {}

        var baz: String
            get() = ""
            set(foo: @Anno(foo) String) {}
    }

    val lambda = { foo: @Anno(foo) String -> }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, const, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, localFunction, nullableType, primaryConstructor, propertyDeclaration, stringLiteral,
thisExpression, typeParameter */
