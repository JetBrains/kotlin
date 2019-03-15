// !LANGUAGE: +VariableDeclarationInWhenSubject

enum class X { A, B, C }

fun test(a: X) =
    when (val subject = a) {
        X.A -> 0
        X.B -> 1
        X.C -> 2
    }

// 1 LOCALVARIABLE subject