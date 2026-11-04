// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INCOMPATIBLE_MODIFIERS
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
open class B1 {
    @A
    private open fun foo() {}
}

class D1 : B1() {
    <!NOTHING_TO_OVERRIDE("foo;  Potential signatures for overriding:In package 'root package' of module '<main>':  --> cannotOverrideInvisibleMember.kt   | 8 | class B1 : Any {   |     ...10 |     fun foo(): Unit   |     ...   | }   |")!>override<!> fun foo() {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, override */
