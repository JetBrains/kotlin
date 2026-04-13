// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class A

@A
class C<T> {
    @A
    operator fun set(x: @A Int, y: @A T) {}
}

fun test(a: C<out CharSequence>) {
    a[1] = <!MEMBER_PROJECTED_OUT("C<out CharSequence>; out; fun set(x: Int, y: T): Unit")!>25<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, functionDeclaration, integerLiteral,
nullableType, operator, outProjection, typeParameter */
