// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83460
// LANGUAGE: +AnnotationAllUseSiteTarget
// FIR_DUMP

class MyClass(
    @all:Deprecated("Obsolete")
    var prop: Int,
) {
    @all:Deprecated("Obsolete")
    var otherProp: Int = 42
}

fun main() {
    val my = MyClass(42)
    my.<!DEPRECATION!>prop<!>
    my.<!DEPRECATION!>prop<!> = 13
    my.<!DEPRECATION!>otherProp<!> = 13
    my.<!DEPRECATION!>otherProp<!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetAll, assignment, classDeclaration, init, integerLiteral,
primaryConstructor, propertyDeclaration, propertyDelegate, setter, starProjection, stringLiteral */
