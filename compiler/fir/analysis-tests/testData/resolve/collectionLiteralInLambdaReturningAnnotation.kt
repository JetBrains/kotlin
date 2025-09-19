// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidArrayLiteralsInNonAnnotationContexts
// DIAGNOSTICS: -UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_WARNING

annotation class Anno

fun runAnno(block: () -> Unit): Anno {
    block()
    return Anno()
}

fun runExpected(block: () -> Array<String>): Anno {
    block()
    return Anno()
}

fun test() {
    // error
    run {
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        Anno()
    }

    // error
    runAnno {
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
    }

    // warning
    runExpected {
        ["42"]
    }

    // error
    run {
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        Anno()
    }

    // error
    run {
        if (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
        Anno()
    }

    // warning
    run {
        if (true) ["42"]
        Anno()
    }

    // error
    run {
        if (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        } else {
            Unit
        }
        Anno()
    }

    // error
    runAnno {
        var res = if (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        } else {
            Unit
        }
    }

    // warning
    runAnno {
        ["42"]<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    // error
    runAnno {
        if (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        } else {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }

    // error
    runAnno {
        if (true) ["42"]
        else {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }

    // warning
    runAnno {
        if (true) ["42"]
        else ["42"]
    }

    // error
    runAnno {
        if (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        } else {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[42]<!>
        }
    }

    // error
    runAnno {
        run {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }

    // error
    runAnno {
        runAnno {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }

    // error
    runAnno {
        run {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        }
    }

    // warning
    runAnno {
        [] as Any
    }

    // error
    runExpected {
        when {
            true -> []
            else -> {
                <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
            }
        }
    }

    // warning
    runExpected {
        when {
            true -> []
            else -> []
        }
    }

    // error
    runAnno {
        try {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        } finally {
        }
    }

    // error
    runAnno {
        val res = try {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        } finally {

        }
    }

    // error
    runAnno {
        val res = try {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        } finally {
        }
    }

    // error
    runExpected {
        try {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        } finally {
        }
    }

    // error
    runAnno {
        try {
        } finally {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        }
    }

    // error
    runAnno {
        try {
            arrayOf("42")
        } catch(_: Exception) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        }
    }

    // error
    runExpected {
        try {
            arrayOf("42")
        } catch(_: Exception) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }

    // error
    runAnno {
        object {
            init {
                <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
            }
        }
    }

    // warning
    runAnno {
        object {
            init {
                if (true) []
            }
        }
    }

    // error
    runAnno {
        fun local() {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        }
    }

    // warning
    runAnno {
        fun local() = []
    }

    // warning
    runAnno {
        val it = fun() = []
        it()
    }

    // warning
    runAnno {
        val it = fun(): Array<String> = []
        it()
    }

    // error
    runAnno {
        val x = fun() {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
        }
        x()
    }

    // warning
    runExpected(fun() = [])

    // error
    runAnno(fun() { <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!> })

    // warning
    runAnno {
        while (true) ["42"]
    }

    // error
    runAnno {
        while (true) {
            <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["42"]<!>
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, anonymousObjectExpression, asExpression,
checkNotNullCall, collectionLiteral, functionDeclaration, functionalType, ifExpression, init, integerLiteral,
lambdaLiteral, localFunction, localProperty, outProjection, propertyDeclaration, stringLiteral, tryExpression,
unnamedLocalVariable, whenExpression, whileLoop */
