// "Migrate backing field syntax" "true"

class Foo {
    var a: Int = 0
        get() = 0
        set(v) { $<caret>a = v }
}
