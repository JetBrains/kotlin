
// WITH_RUNTIME

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline var prop: String
    get() = "12"
    set(value) {}

inline var prop2: String
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
    get() = "12"
    set(value) {}


inline val <reified Z> Z.extProp: String
    get() = "123"

class Foo {

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline var prop: String
        get() = "12"
        set(value) {}

    inline var prop2: String
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        @kotlin.internal.InlineOnly
        get() = "12"
        set(value) {}


    inline val <reified Z> Z.extProp: String
        get() = "123"

}
