@file:Suppress("INVISIBLE_REFERENCE")
@kotlin.internal.DoNotInlineOnFirstStage
inline fun inlineFunction() = "inlineFunction.v1"

@kotlin.internal.DoNotInlineOnFirstStage
inline fun inlineFunctionWithParam(param: String = "default.v1") = "inlineFunctionWithParam.v1: $param"

@kotlin.internal.DoNotInlineOnFirstStage
context(c: String)
inline fun String.inlineExtensionFunction() = "$this.inlineExtensionFunction.v1 with context $c"

inline val excludedReadOnlyInlinePropertyGetter: String
    @kotlin.internal.DoNotInlineOnFirstStage
    get() = "excludedReadOnlyInlinePropertyGetter.v1"

context(c: String)
inline val String.excludedReadOnlyInlineExtensionPropertyGetter: String
    @kotlin.internal.DoNotInlineOnFirstStage
    get() = "$this.excludedReadOnlyInlineExtensionPropertyGetter.v1 with context $c"

var _excludedReadWriteInlinePropertySetter = ""
inline var excludedReadWriteInlinePropertySetter: String
    get() = _excludedReadWriteInlinePropertySetter
    @kotlin.internal.DoNotInlineOnFirstStage
    set(value) {
        _excludedReadWriteInlinePropertySetter = "$value.v1"
    }

var _excludedReadWriteInlineExtensionPropertySetter = ""
context(c: String)
inline var String.excludedReadWriteInlineExtensionPropertySetter: String
    get() = _excludedReadWriteInlineExtensionPropertySetter
    @kotlin.internal.DoNotInlineOnFirstStage
    set(value) {
        _excludedReadWriteInlineExtensionPropertySetter = "$this.$value.v1 with context $c"
    }

class C {
    @kotlin.internal.DoNotInlineOnFirstStage
    inline fun inlineClassFunction() = "inlineClassFunction.v1"

    @kotlin.internal.DoNotInlineOnFirstStage
    inline fun inlineClassFunctionWithParam(param: String = "default.v1") = "inlineClassFunctionWithParam.v1: $param"

    @kotlin.internal.DoNotInlineOnFirstStage
    context(c: String)
    inline fun String.inlineClassExtensionFunction() = "$this.inlineClassExtensionFunction.v1 with context $c"

    inline val excludedReadOnlyInlineClassPropertyGetter: String
        @kotlin.internal.DoNotInlineOnFirstStage
        get() = "excludedReadOnlyInlineClassPropertyGetter.v1"

    context(c: String)
    inline val String.excludedReadOnlyInlineClassExtensionPropertyGetter: String
        @kotlin.internal.DoNotInlineOnFirstStage
        get() = "$this.excludedReadOnlyInlineClassExtensionPropertyGetter.v1 with context $c"

    var _excludedReadWriteInlineClassPropertySetter = ""
    inline var excludedReadWriteInlineClassPropertySetter: String
        get() = _excludedReadWriteInlineClassPropertySetter
        @kotlin.internal.DoNotInlineOnFirstStage
        set(value) {
            _excludedReadWriteInlineClassPropertySetter = "$value.v1"
        }

    var _excludedReadWriteInlineClassExtensionPropertySetter = ""
    context(c: String)
    inline var String.excludedReadWriteInlineClassExtensionPropertySetter: String
        get() = _excludedReadWriteInlineClassExtensionPropertySetter
        @kotlin.internal.DoNotInlineOnFirstStage
        set(value) {
            _excludedReadWriteInlineClassExtensionPropertySetter = "$this.$value.v1 with context $c"
        }
}
