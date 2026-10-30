// LL_FIR_DIVERGENCE
// `INLINE_FROM_HIGHER_PLATFORM` is not reported: LL analyzes the dependency module from sources, and
// `FirJvmInlineTargetQualifiedAccessChecker` skips source callables since only class files carry the version.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
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
    inlineFun {}
    inlineGetter
    inlineGetter = 1

    inlineSetter
    inlineSetter = 1

    allInline
    allInline = 1

    inlineProperty
    inlineProperty = 1

    val base = Base()
    base.inlineFunBase {}
    base.inlineGetterBase
    base.inlineGetterBase = 1

    base.inlineSetterBase
    base.inlineSetterBase = 1

    base.allInlineBase
    base.allInlineBase = 1

    base.inlinePropertyBase
    base.inlinePropertyBase = 1
}

class Derived : Base() {
    fun test() {
        inlineFunBase {}
        inlineGetterBase
        inlineGetterBase = 1

        inlineSetterBase
        inlineSetterBase = 1

        allInlineBase
        allInlineBase = 1

        inlinePropertyBase
        inlinePropertyBase = 1
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, getter, inline, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration, setter */
