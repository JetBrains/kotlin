// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
annotation class PropertyAnnotation
annotation class FieldAnnotation
annotation class GetAnnotation
annotation class SetAnnotation
annotation class SetparamAnnotation
annotation class ExplicitGetAnnotation
annotation class ExplicitSetAnnotation
annotation class ExplicitSetparamAnnotation

@property:PropertyAnnotation
@field:FieldAnnotation
@get:GetAnnotation
@set:SetAnnotation
@setparam:SetparamAnnotation
var x: Int = 0
    @ExplicitGetAnnotation
    get() = field
    @ExplicitSetAnnotation
    s<caret>et(@ExplicitSetparamAnnotation value) {
        field = value
    }
