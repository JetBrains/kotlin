inline val inlineProperty: String
    get() = "inlineProperty.v1"

context(c: String)
inline val String.inlineExtensionProperty: String
    get() = "$this.inlineExtensionProperty.v1 with context $c"

class C {
    inline val inlineClassProperty: String
        get() = "inlineClassProperty.v1"

    context(c: String)
    inline val String.inlineClassExtensionProperty: String
        get() = "$this.inlineClassExtensionProperty.v1 with context $c"
}