// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// PRETTY_RENDERER_OPTION: FULLY_EXPANDED_TYPES

@Target(AnnotationTarget.TYPE)
annotation class Anno1
@Target(AnnotationTarget.TYPE)
annotation class Anno2
@Target(AnnotationTarget.TYPE)
annotation class Anno3

interface BaseInterface

typealias FirstTypeAlias = @Anno1 BaseInterface
typealias SecondTypeAlias = @Anno2 FirstTypeAlias

val obj = obje<caret>ct : @Anno3 SecondTypeAlias {

}
