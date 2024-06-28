// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

import java.lang.Deprecated

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
@field:Deprecated
@get:GetAnnotation
@set:SetAnnotation
@setparam:SetparamAnnotation
var x: Int = 0
    @ExplicitGetAnnotation
    get() = <caret>field
    @ExplicitSetAnnotation
    set(@ExplicitSetparamAnnotation value) {
        field = value
    }
