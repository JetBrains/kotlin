@Target(AnnotationTarget.FUNCTION)
annotation class FunAnn

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnn

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn

fun bar(arg: () -> Int) = arg()

inline fun fast(arg: () -> Int) = arg()

inline fun fast2(x: Int, arg: () -> Int) = x + arg()

@FunAnn fun gav() = 13

fun foo(arg: Int) {
    // Literal is annotatable
    bar @FunAnn { arg }
    // Annotatable in principle but useless, fast is inline
    fast <!NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION!>@FunAnn<!> { arg }
    fast2(1, <!NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION!>@FunAnn<!> { arg })
    // Source annotation, ok
    fast @SourceAnn { arg }
    fast2(1, @SourceAnn { arg })
    // Expression annotation, ok
    fast @ExprAnn { arg }
    fast2(1, @ExprAnn { arg })
    // Function expression too
    val f = @FunAnn fun(): Int { return 42 }
    // But here, f and gav should be annotated instead
    bar(<!WRONG_ANNOTATION_TARGET!>@FunAnn<!> f)
    bar(<!WRONG_ANNOTATION_TARGET!>@FunAnn<!> ::gav)
    // Function expression, ok
    fast(f)
}
