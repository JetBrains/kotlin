// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        val columns: List<String>
        columns = ["!"]
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        var columns: MutableList<String>
        columns = ["!"]
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        var columns: MutableList<Int> = []
        columns = []
    }

    buildList {
        var columns: MutableList<Int> = []
        add("!")
        columns = []
    }

    buildList {
        val columns: MutableList<Int>
        columns = when {
            true -> []
            else -> [1, 2, 3]
        }
        addAll(columns)
    }

    buildList {
        val columns: List<Int>
        columns = when {
            true -> []
            else -> [1, 2, 3]
        }
        addAll(columns)
    }

    buildList {
        val columns: List<Int>
        columns = when {
            true -> when {
                true -> []
                else -> [1, 2, 3]
            }
            else -> [4, 5, 6]
        }
        addAll(columns)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
