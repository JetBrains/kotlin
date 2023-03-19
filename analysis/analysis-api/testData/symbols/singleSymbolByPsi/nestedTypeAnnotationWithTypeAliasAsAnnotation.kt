// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// PRETTY_RENDERER_OPTION: FULLY_EXPANDED_TYPES
@Target(AnnotationTarget.TYPE)
annotation class Anno1(val s: String)
@Target(AnnotationTarget.TYPE)
annotation class Anno2

@Target(AnnotationTarget.TYPE)
annotation class BaseAnnotation

typealias FirstTypeAlias = @Anno1("s") BaseAnnotation
typealias SecondTypeAlias = @Anno2 FirstTypeAlias

fun <T> T.f<caret>oo2(): List<List<@SecondTypeAlias T>>? = null
