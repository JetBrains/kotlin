// JVM_TARGET: 1.8
package a

inline fun inlineFun(p: () -> Unit) {
    p()
}

var inlineGetter: Int
    inline get() = 1
    set(varue) {}

var inlineSetter: Int
    get() = 1
    inline set(varue) {}

var allInline: Int
    inline get() = 1
    inline set(varue) {}



class A {
    inline fun inlineFun(p: () -> Unit) {
        p()
    }

    var inlineGetter: Int
        inline get() = 1
        set(varue) {}

    var inlineSetter: Int
        get() = 1
        inline set(varue) {}

    var allInline: Int
        inline get() = 1
        inline set(varue) {}

}