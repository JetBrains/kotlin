inline var topLevelProperty: String
    get() = "topLevelProperty"
    set(value) {
        print(value)
    }

context(c: String)
inline var String.topLevelPropertyWithReceiver: String
    get() = "topLevelPropertyWithReceiver"
    set(value) {
        print("$c $this $value")
    }

class C {
    inline var classProperty: String
        get() = "classProperty"
        set(value) {
            print(value)
        }

    context(c: String)
    inline var String.classPropertyWithReceiver: String
        get() = "classPropertyWithReceiver"
        set(value) {
            print("$c $this $value")
        }
}
