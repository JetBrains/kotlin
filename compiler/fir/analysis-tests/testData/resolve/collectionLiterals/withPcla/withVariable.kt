// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun <X> id(x: X) = x

fun <R> runIf(b: Boolean, block: () -> R): R? = if (!b) null else block()

fun test() {
    buildBox {
        x = setOf(1, 2, 3)
        x = [1, 2, 3]
    }

    buildBox {
        x = setOf(1, 2, 3)
        x = ["1", "2", "3"]
    }

    buildBox {
        x = [1, 2, 3]
        x = setOf(1, 2, 3)
    }

    buildBox {
        x = [1, 2, 3]
    }

    <!CANNOT_INFER_PARAMETER_TYPE("Z")!>buildBox<!> {
        x = <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>
    }

    buildBox {
        x = ["1", "2", "3"]
    }

    buildBox {
        x = [1, 2, 3]
        x = [1, 2, 3]
    }

    buildBox {
        x = [1, 2, 3]
        x = ["1", "2", "3"]
    }

    buildBox {
        x = []
        x = [1, 2, 3]
    }

    <!CANNOT_INFER_PARAMETER_TYPE("Z")!>buildBox<!> {
        x = <!CANNOT_INFER_PARAMETER_TYPE("T")!>[x]<!>
    }
}

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
        x <!ASSIGNMENT_TYPE_MISMATCH("Set<String>; Set<Int>")!>=<!> setOf(1, 2, 3)
        x = ["!"]
        <!TYPE_MISMATCH("String; Int")!>x<!>.size
    }<!>

    <!TYPE_MISMATCH("String; Int")!>buildBox {
        x = ["!"]
        x <!ASSIGNMENT_TYPE_MISMATCH("Collection<String>; Set<Int>")!>=<!> setOf(1, 2, 3)
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

fun testWithoutAssignment() {
    buildBox {
        [1, 2, 3]
        x = 42
    }

    buildBox {
        x = 42
        [1, 2, 3]
    }

    buildBox {
        [x]
        x = 42
    }

    <!CANNOT_INFER_PARAMETER_TYPE("Z")!>buildBox<!> {
        [1, 2, 3]
    }

    <!CANNOT_INFER_PARAMETER_TYPE("Z")!>buildBox<!> {
        <!CANNOT_INFER_PARAMETER_TYPE("T")!>[x]<!>
    }
}

fun testWithId() {
    buildBox {
        x = id([1, 2, 3])
    }

    buildBox {
        x = id([1, 2, 3])
        x = id(["1", "2", "3"])
    }

    buildBox {
        x = setOf(1, 2, 3)
        x = id(["1", "2", "3"])
    }
}

fun testWithElvis() {
    buildBox {
        x = runIf(true) { setOf() } ?: [1, 2, 3]
    }

    buildBox {
        x = runIf(true) { [1, 2, 3] } ?: ["1", "2", "3"]
    }

    <!CANNOT_INFER_PARAMETER_TYPE("Z")!>buildBox<!> {
        x = <!CANNOT_INFER_PARAMETER_TYPE("R")!>runIf<!>(true) { <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!> } ?: <!CANNOT_INFER_PARAMETER_TYPE("T")!>[]<!>
    }
}

fun testWithWhen() {
    buildBox {
        x = when {
            true -> []
            else -> []
        }
        x = setOf(42)
    }

    buildBox {
        x = when {
            true -> []
            else -> setOf(42)
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, typeParameter, typeWithExtension */
