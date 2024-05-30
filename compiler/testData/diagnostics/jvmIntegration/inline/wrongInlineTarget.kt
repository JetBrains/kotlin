// MODULE: library
// JVM_TARGET: 11
// FILE: a.kt
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

inline var inlineProperty: Int
    get() = 1
    set(varue) { varue.hashCode() }

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

    inline var inlinePropertyBase: Int
        get() = 1
        set(varue) { varue.hashCode() }
}

// MODULE: main(library)
// JVM_TARGET: 1.8
// FILE: source.kt
package usage

import a.*

fun baz() {
    <!INLINE_FROM_HIGHER_PLATFORM!>inlineFun<!> {}
    <!INLINE_FROM_HIGHER_PLATFORM!>inlineGetter<!>
    <!INLINE_FROM_HIGHER_PLATFORM!>inlineGetter<!> = 1

    inlineSetter
    <!INLINE_FROM_HIGHER_PLATFORM!>inlineSetter<!> = 1

    <!INLINE_FROM_HIGHER_PLATFORM!>allInline<!>
    <!INLINE_FROM_HIGHER_PLATFORM!>allInline<!> = 1

    <!INLINE_FROM_HIGHER_PLATFORM!>inlineProperty<!>
    <!INLINE_FROM_HIGHER_PLATFORM!>inlineProperty<!> = 1

    val base = Base()
    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunBase<!> {}
    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlineGetterBase<!>
    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlineGetterBase<!> = 1

    base.inlineSetterBase
    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlineSetterBase<!> = 1

    base.<!INLINE_FROM_HIGHER_PLATFORM!>allInlineBase<!>
    base.<!INLINE_FROM_HIGHER_PLATFORM!>allInlineBase<!> = 1

    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlinePropertyBase<!>
    base.<!INLINE_FROM_HIGHER_PLATFORM!>inlinePropertyBase<!> = 1
}

class Derived : Base() {
    fun test() {
        <!INLINE_FROM_HIGHER_PLATFORM!>inlineFunBase<!> {}
        <!INLINE_FROM_HIGHER_PLATFORM!>inlineGetterBase<!>
        <!INLINE_FROM_HIGHER_PLATFORM!>inlineGetterBase<!> = 1

        inlineSetterBase
        <!INLINE_FROM_HIGHER_PLATFORM!>inlineSetterBase<!> = 1

        <!INLINE_FROM_HIGHER_PLATFORM!>allInlineBase<!>
        <!INLINE_FROM_HIGHER_PLATFORM!>allInlineBase<!> = 1

        <!INLINE_FROM_HIGHER_PLATFORM!>inlinePropertyBase<!>
        <!INLINE_FROM_HIGHER_PLATFORM!>inlinePropertyBase<!> = 1
    }
}
