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
var prop: Int = 1

annotation class DelegateAnnotation

@delegate:DelegateAnnotation
val lazyProperty by lazy { 1 }
