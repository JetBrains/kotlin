// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86332
// MODULE: a
// FILE: a.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

// MODULE: b(a)
// FILE: b.kt
fun f(): @Anno String = ""

fun nullableF(): @Anno String? = ""

fun interface Sam {
    fun foo(x: @Anno String)
}

// MODULE: c(b)
// FILE: c.kt
fun g() = <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>f<!>()

val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{}<!>

fun <T> T.accept(arg: (T) -> Unit) {}

inline fun <reified T> T.acceptNoInline(noinline arg: (T) -> Unit) {}

inline fun <reified T> T.acceptCrossInline(crossinline arg: (T) -> Unit) {}

fun local() {
    val x = f()

    val nullableX = nullableF()

    x.let {

    }

    nullableX?.let {

    }

    x.accept() <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{

    }<!>

    nullableX?.accept() <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{

    }<!>

    x.acceptNoInline() <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{

    }<!>

    nullableX?.acceptNoInline() <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{

    }<!>

    x.acceptCrossInline() {

    }

    nullableX?.acceptCrossInline() {

    }

    val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{}<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, safeCall, stringLiteral */
