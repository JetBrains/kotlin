// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// WITH_STDLIB

annotation class PropertyAnnotation
annotation class FieldAnnotation
annotation class GetAnnotation
annotation class SetAnnotation
annotation class SetparamAnnotation

@property:PropertyAnnotation
@field:FieldAnnotation
@get:GetAnnotation
@set:SetAnnotation
@setparam:SetparamAnnotation
var pr<caret>op: Int = 1
