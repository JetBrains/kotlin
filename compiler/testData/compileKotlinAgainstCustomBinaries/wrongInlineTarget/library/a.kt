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



open class Base {
    inline fun inlineFunBase(p: () -> Unit) {
        p()
    }

    var inlineGetterBase: Int
        inline get() = 1
        set(varue) { varue.hashCode() }

    var inlineSetterBase: Int
        get() = 1
        inline set(varue) { varue.hashCode() }

    var allInlineBase: Int
        inline get() = 1
        inline set(varue) { varue.hashCode() }
}