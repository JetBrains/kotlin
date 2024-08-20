// FIR_IDENTICAL
fun invokeLater(x: () -> Unit) {
    x()
}

fun nestedFinallyAndLambda1() {
    var x: String
    invokeLater {
        x = ""
        invokeLater {
            try {
            } finally {
                x.length
            }
        }
    }
}

fun nestedFinallyAndLambda2() {
    var x: String
    invokeLater {
        x = ""
        try {
        } finally {
            invokeLater {
                x.length
            }
        }
    }
}

fun nestedFinallyAndLambda3() {
    var x: String
    try {
    } finally {
        invokeLater {
            x = ""
            invokeLater {
                x.length
            }
        }
    }
}
