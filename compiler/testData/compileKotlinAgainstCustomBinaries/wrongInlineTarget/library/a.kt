// JVM_TARGET: 1.8
package a

inline fun inlineFun(p: () -> Unit) {
    p()
}

var inlineGetter: Int
    inline get() = 1
    set(varue) { varue.hashCode() }

var inlineSetter: Int
    get() = 1
    inline set(varue) { varue.hashCode() }

var allInline: Int
    inline get() = 1
    inline set(varue) { varue.hashCode() }



class A {
    inline fun inlineFun(p: () -> Unit) {
        p()
    }

    var inlineGetter: Int
        inline get() = 1
        set(varue) { varue.hashCode() }

    var inlineSetter: Int
        get() = 1
        inline set(varue) { varue.hashCode() }

    var allInline: Int
        inline get() = 1
        inline set(varue) { varue.hashCode() }

}