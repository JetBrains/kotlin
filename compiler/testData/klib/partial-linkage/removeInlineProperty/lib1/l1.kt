var _topLevelProperty = ""
inline var topLevelProperty: String
    get() = "topLevelProperty"
    set(value) {
        _topLevelProperty = value
    }

var _topLevelPropertyWithReceiver = ""
context(c: String)
inline var String.topLevelPropertyWithReceiver: String
    get() = "topLevelPropertyWithReceiver"
    set(value) {
        _topLevelPropertyWithReceiver = "$this.$value with context $c"
    }

class C {
    var _classProperty = ""
    inline var classProperty: String
        get() = "classProperty"
        set(value) {
            _classProperty = value
        }

    var _classPropertyWithReceiver = ""
    context(c: String)
    inline var String.classPropertyWithReceiver: String
        get() = "classPropertyWithReceiver"
        set(value) {
            _classPropertyWithReceiver = "$this.$value with context $c"
        }
}
