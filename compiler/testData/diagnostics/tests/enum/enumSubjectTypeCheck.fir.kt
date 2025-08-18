// RUN_PIPELINE_TILL: FRONTEND
// See KT-14705

enum class En { A, B, С }

fun foo() {
    // nullable variable
    val en2: Any? = En.A
    if (en2 is En) {
        when (en2) {
            En.A -> {}
            En.B -> {}
            En.С -> {}
        }
    }

    // not nullable variable
    val en1: Any = En.A
    if (en1 is En) {
        when (en1) {
            En.A -> {}
            En.B -> {}
            En.С -> {}
        }
    }
}

enum class En2 { D, E, F }

fun useEn(x: En) = x
fun useEn2(x: En2) = x

fun bar(x: Any) {
    if (x is En && <!IMPOSSIBLE_IS_CHECK_ERROR!>x is En2<!>) {
        when (x) {
            En.A -> useEn(x)
            En2.D -> useEn2(x)
            else -> {}
        }
    }
}

/* GENERATED_FIR_TAGS: andExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression,
intersectionType, isExpression, localProperty, nullableType, propertyDeclaration, smartcast, whenExpression,
whenWithSubject */
