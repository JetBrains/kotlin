val <reified T> T.foo: String
    inline get() {
        return if (T::class.simpleName == "String") "O" else "fail"
    }

inline var <reified T> T.bar: String
    get() {
        return if (T::class.simpleName == "String") "K" else "fail"
    }
    set(v) { }

fun box(): String {
    return "".foo + "".bar
}