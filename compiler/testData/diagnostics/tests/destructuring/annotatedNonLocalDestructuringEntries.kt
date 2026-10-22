// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// FIR_DUMP
// ISSUE: KT-74654, KT-90199

annotation class Ann(val s: String)

@Target(AnnotationTarget.CLASS)
annotation class ClassOnly

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val s: String)

const val x = "str"

data class X(val a: Int, val b: Int)

val <!SYNTAX!>(@Ann(x) a, @Ann(x) b)<!> = X(1, 2)

val <!SYNTAX!>(@Ann(<!UNRESOLVED_REFERENCE!>y<!>) c: Int, @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!WRONG_ANNOTATION_TARGET!>@ClassOnly<!> d: @TypeAnn(x) Int)<!> = X(1, 2)

@Ann(x)
val <!SYNTAX!>[@Ann(x) e, f]<!> = X(1, 2)

class C {
    val <!SYNTAX!>(@Ann(x) a, @Ann(x) b)<!> = X(1, 2)

    val <!SYNTAX!>(@Ann(<!UNRESOLVED_REFERENCE!>y<!>) c: Int, @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!WRONG_ANNOTATION_TARGET!>@ClassOnly<!> d: @TypeAnn(x) Int)<!> = X(1, 2)

    @Ann(x)
    val <!SYNTAX!>[@Ann(x) e, f]<!> = X(1, 2)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, data, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
