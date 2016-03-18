package test

inline fun annotatedWith2(crossinline predicate: () -> Boolean) =
        { any { predicate() } }()


inline fun annotatedWith(crossinline predicate: () -> Boolean) =
        annotatedWith2 { predicate() }


inline fun any(s: () -> Boolean) {
    s()
}