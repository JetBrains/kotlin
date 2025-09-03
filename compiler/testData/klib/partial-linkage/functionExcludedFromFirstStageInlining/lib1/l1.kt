@kotlin.internal.DoNotInlineOnFirstStage
inline fun inlineFunction() = "inlineFunction.v1"

@kotlin.internal.DoNotInlineOnFirstStage
inline fun inlineFunctionWithParam(param: String = "default.v1") = "inlineFunctionWithParam.v1: $param"

@kotlin.internal.DoNotInlineOnFirstStage
context(c: String)
inline fun String.inlineExtensionFunction() = "$this.inlineExtensionFunction.v1 with context $c"

@kotlin.internal.DoNotInlineOnFirstStage
inline val excludedReadOnlyInlineProperty: String
    get() = "excludedReadOnlyInlineProperty.v1"

@kotlin.internal.DoNotInlineOnFirstStage
context(c: String)
inline val String.excludedReadOnlyInlineExtensionProperty: String
    get() = "$this.excludedReadOnlyInlineExtensionProperty.v1 with context $c"

inline val excludedReadOnlyInlinePropertyGetter: String
    @kotlin.internal.DoNotInlineOnFirstStage
    get() = "excludedReadOnlyInlinePropertyGetter.v1"

context(c: String)
inline val String.excludedReadOnlyInlineExtensionPropertyGetter: String
    @kotlin.internal.DoNotInlineOnFirstStage
    get() = "$this.excludedReadOnlyInlineExtensionPropertyGetter.v1 with context $c"

var _excludedReadWriteInlineProperty = ""
@kotlin.internal.DoNotInlineOnFirstStage
inline var excludedReadWriteInlineProperty: String
    get() = _excludedReadWriteInlineProperty
    set(value) {
        _excludedReadWriteInlineProperty = "$value.v1"
    }

var _excludedReadWriteInlineExtensionProperty = ""
@kotlin.internal.DoNotInlineOnFirstStage
context(c: String)
inline var String.excludedReadWriteInlineExtensionProperty: String
    get() = _excludedReadWriteInlineExtensionProperty
    set(value) {
        _excludedReadWriteInlineExtensionProperty = "$this.$value.v1 with context $c"
    }

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

    @kotlin.internal.DoNotInlineOnFirstStage
    inline val excludedReadOnlyInlineClassProperty: String
        get() = "excludedReadOnlyInlineClassProperty.v1"

    @kotlin.internal.DoNotInlineOnFirstStage
    context(c: String)
    inline val String.excludedReadOnlyInlineClassExtensionProperty: String
        get() = "$this.excludedReadOnlyInlineClassExtensionProperty.v1 with context $c"

    inline val excludedReadOnlyInlineClassPropertyGetter: String
        @kotlin.internal.DoNotInlineOnFirstStage
        get() = "excludedReadOnlyInlineClassPropertyGetter.v1"

    context(c: String)
    inline val String.excludedReadOnlyInlineClassExtensionPropertyGetter: String
        @kotlin.internal.DoNotInlineOnFirstStage
        get() = "$this.excludedReadOnlyInlineClassExtensionPropertyGetter.v1 with context $c"

    var _excludedReadWriteInlineClassProperty = ""
    @kotlin.internal.DoNotInlineOnFirstStage
    inline var excludedReadWriteInlineClassProperty: String
        get() = _excludedReadWriteInlineClassProperty
        set(value) {
            _excludedReadWriteInlineClassProperty = "$value.v1"
        }

    var _excludedReadWriteInlineClassExtensionProperty = ""
    @kotlin.internal.DoNotInlineOnFirstStage
    context(c: String)
    inline var String.excludedReadWriteInlineClassExtensionProperty: String
        get() = _excludedReadWriteInlineClassExtensionProperty
        set(value) {
            _excludedReadWriteInlineClassExtensionProperty = "$this.$value.v1 with context $c"
        }

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
