@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

fun bar(arg: () -> Int) = arg()

@FunAnn fun gav() = 13

fun foo(arg: Int) {
    // Literal is annotatable
    bar @FunAnn { arg }
    // Function expression too
    val f = @FunAnn fun(): Int { return 42 }
    // But here, f and gav should be annotated instead
    bar(<!WRONG_ANNOTATION_TARGET!>@FunAnn<!> f)
    bar(<!WRONG_ANNOTATION_TARGET!>@FunAnn<!> ::gav)
}