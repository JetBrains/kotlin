// IGNORE_BACKEND: JVM_IR
inline val <reified Z> Z.extProp: String
    get() = "123"

class Foo {
    inline val <reified Z> Z.extProp: String
        get() = "456"
}
