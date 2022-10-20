// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// WITH_STDLIB

annotation class PropertyAnnotation
annotation class ExplicitPropertyAnnotation
annotation class DelegateAnnotation

@property:ExplicitPropertyAnnotation
@delegate:DelegateAnnotation
@PropertyAnnotation
val lazyPr<caret>operty by lazy { 1 }
