// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val a: Array<String>, val b: String)

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoWrong(val a: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>List<() -> List<String>><!>)

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoComplex(val a: Array<AnnoComplex>)

fun noop() = Unit

fun test() {
    buildBox {
        @Anno(a = [], b = "")
        x = 42
    }
    buildBox {
        @Anno(a = [""], b = "")
        x = x
        x = 42
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        @Anno(a = [], b = "")
        x = x
    }
    buildBox {
        @Anno(a = [], b = <!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>[]<!>)
        x = 42
    }
    buildBox {
        @Anno(a = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>["!"]<!>]<!>, b = "")
        x = 42
    }
    buildBox {
        @Anno(a = [], b = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
        noop()
    }
    buildBox {
        @Anno(a = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>]<!>, b = "")
        noop()
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        @Anno(a = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>[x]<!>]<!>, b = "")
        noop()
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        @AnnoWrong(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>[{ [""] }]<!>)
        noop()
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        @AnnoComplex(a = [AnnoComplex([AnnoComplex(arrayOf(AnnoComplex([AnnoComplex(arrayOf(AnnoComplex(arrayOf(AnnoComplex([])))))])))])])
        noop()
    }
    buildBox {
        @AnnoComplex(a = [AnnoComplex([AnnoComplex(arrayOf(AnnoComplex([AnnoComplex(arrayOf(AnnoComplex(arrayOf(AnnoComplex([])))))])))])])
        x = 42
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, collectionLiteral, functionDeclaration, functionalType,
integerLiteral, interfaceDeclaration, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter, typeWithExtension */
