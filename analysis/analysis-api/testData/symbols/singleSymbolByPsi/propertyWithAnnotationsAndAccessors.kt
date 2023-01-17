// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// PRETTY_RENDERERE_OPTION: BODY_WITH_MEMBERS

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
var x<caret>: Int = 0
    @ExplicitGetAnnotation
    get() = field
    @ExplicitSetAnnotation
    set(@ExplicitSetparamAnnotation value) {
        field = value
    }
