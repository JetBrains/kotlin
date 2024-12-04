// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
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

val obj = ob<caret>ject : @Anno3 SecondTypeAlias()
