import kotlin.internal.ExcludedFromFirstStageInlining

@ExcludedFromFirstStageInlining
inline val excludedInlineProperty: String
    get() = "excludedInlineProperty.v1"

@ExcludedFromFirstStageInlining
context(c: String)
inline val String.excludedInlineExtensionProperty: String
    get() = "$this.excludedInlineExtensionProperty.v1 with context $c"

inline val excludedInlinePropertyGetter: String
    @ExcludedFromFirstStageInlining
    get() = "excludedInlinePropertyGetter.v1"

context(c: String)
inline val String.excludedInlineExtensionPropertyGetter: String
    @ExcludedFromFirstStageInlining
    get() = "$this.excludedInlineExtensionPropertyGetter.v1 with context $c"

class C {
    @ExcludedFromFirstStageInlining
    inline val excludedInlineClassProperty: String
        get() = "excludedInlineClassProperty.v1"

    @ExcludedFromFirstStageInlining
    context(c: String)
    inline val String.excludedInlineClassExtensionProperty: String
        get() = "$this.excludedInlineClassExtensionProperty.v1 with context $c"

    inline val excludedInlineClassPropertyGetter: String
        @ExcludedFromFirstStageInlining
        get() = "excludedInlineClassPropertyGetter.v1"

    context(c: String)
    inline val String.excludedInlineClassExtensionPropertyGetter: String
        @ExcludedFromFirstStageInlining
        get() = "$this.excludedInlineClassExtensionPropertyGetter.v1 with context $c"
}