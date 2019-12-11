// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    <!UNRESOLVED_REFERENCE!>{
        p()
    }()<!>
}

inline fun <R> inlineFun(p: () -> R) {
    <!UNRESOLVED_REFERENCE!>{
        p()
    }()<!>
}
