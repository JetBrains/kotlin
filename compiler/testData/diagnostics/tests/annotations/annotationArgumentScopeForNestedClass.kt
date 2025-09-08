// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76357

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

const val CONST = 1

class MyClass {
    val CONST = ""
    @Anno(CONST)
    class NestedClass(
        @Anno(CONST) val a: String,
    ) : @Anno(CONST) Any() {
        @Anno(CONST)
        fun foo() {}
    }

    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>)
    inner class InnerClass(
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>) val a: String,
    ) : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>) Any() {
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST<!>)
        fun foo() {}
    }
}

open class SuperClass {
    companion object {
        const val CONST2 = 1
    }
}

class Subclass : SuperClass() {
    val CONST2 = "str"

    @Anno(CONST2)
    class NestedClass(
        @Anno(CONST2) val a: String,
    ) : @Anno(CONST2) Any() {
        @Anno(CONST2)
        fun foo() {}
    }

    @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST2<!>)
    inner class InnerClass(
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST2<!>) val a: String,
    ) : @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST2<!>) Any() {
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>CONST2<!>)
        fun foo() {}
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, const, functionDeclaration, inner,
integerLiteral, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
