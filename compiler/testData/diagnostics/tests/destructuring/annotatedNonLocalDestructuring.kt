// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-74654

annotation class Ann(val s: String)

@Target(AnnotationTarget.CLASS)
annotation class ClassOnly

const val x = "str"

data class X(val a: Int, val b: Int)

@Ann(x)
val <!SYNTAX!>(a, b)<!> = X(1, 2)

@Ann(y)
@Unresolved
@ClassOnly
@property:Ann(x)
val <!SYNTAX!>(c, d)<!> = X(1, 2)

class C {
    @Ann(x)
    val <!SYNTAX!>(a, b)<!> = X(1, 2)

    @Ann(y)
    @Unresolved
    @ClassOnly
    @property:Ann(x)
    val <!SYNTAX!>(c, d)<!> = X(1, 2)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, data, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
