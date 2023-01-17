// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// PRETTY_RENDERER_OPTION: FULLY_EXPANDED_TYPES

@Target(AnnotationTarget.TYPE)
annotation class Anno1
@Target(AnnotationTarget.TYPE)
annotation class Anno2
@Target(AnnotationTarget.TYPE)
annotation class Anno3

open class BaseClass

typealias FirstTypeAlias = @Anno1 BaseClass
typealias SecondTypeAlias = @Anno2 FirstTypeAlias

class F<caret>oo : @Anno3 SecondTypeAlias()
