// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88681
// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_INFERENCE_LOGS: MARKDOWN
// The cases whose behavior is changed by EliminateSecondKindIncorporation (see KT-88681):
// the on-demand semi-fixation of Z over several not-fixed element variables loses the
// element type (Set<*> instead of Set<Int>). Extracted from withVariable.kt to keep the
// inference-log dump small.

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun testWithSemifixation() {
    buildBox {
        x = [1, 2, 3]
        x.size
    }

    buildBox {
        x = setOf(1, 2, 3)
        x = [1, 2, 3]
        x.size
    }

    // TODO: KT-84797
    <!TYPE_MISMATCH("String; Int")!>buildBox {
        x = <!ASSIGNMENT_TYPE_MISMATCH("Set<String>; Set<Int>")!>setOf<!>(1, 2, 3)
        x = ["!"]
        <!TYPE_MISMATCH("String; Int")!>x<!>.size
    }<!>

    <!TYPE_MISMATCH("String; Int")!>buildBox {
        x = ["!"]
        x = <!ASSIGNMENT_TYPE_MISMATCH("Collection<String>; Set<Int>")!>setOf<!>(1, 2, 3)
        <!TYPE_MISMATCH("String; Int")!>x<!>.size
    }<!>

    buildBox {
        x = [1, 2, 3]
        x = ["1", "2", "3"]
        x.size
    }

    buildBox {
        x = [1, 2, 3]
        x.size
        x = [<!ARGUMENT_TYPE_MISMATCH("String; Int")!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH("String; Int")!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH("String; Int")!>"3"<!>]
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
intersectionType, lambdaLiteral, nullableType, propertyDeclaration, starProjection, stringLiteral, typeParameter,
typeWithExtension */
