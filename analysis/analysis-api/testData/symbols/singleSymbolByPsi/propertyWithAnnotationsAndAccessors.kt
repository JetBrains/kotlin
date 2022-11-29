// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// PRETTY_RENDERING_MODE: RENDER_SYMBOLS_NESTED

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
