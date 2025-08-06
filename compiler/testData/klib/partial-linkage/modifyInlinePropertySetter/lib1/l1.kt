var _inlineProperty = ""
inline var inlineProperty: String
    get() = _inlineProperty
    set(value) {
        _inlineProperty = value + ".v1"
    }


var _inlineExtensionProperty = ""
context(c: String)
inline var String.inlineExtensionProperty: String
    get() = _inlineExtensionProperty
    set(value) {
        _inlineExtensionProperty = "$this.$value.v1 with context $c"
    }

class C {
    var _inlineClassProperty = ""
    inline var inlineClassProperty: String
        get() = _inlineClassProperty
        set(value) {
            _inlineClassProperty = value + ".v1"
        }

    var _inlineClassExtensionProperty = ""
    context(c: String)
    inline var String.inlineClassExtensionProperty: String
        get() = _inlineClassExtensionProperty
        set(value) {
            _inlineClassExtensionProperty = "$this.$value.v1 with context $c"
        }
}