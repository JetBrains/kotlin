package inlineFun

inline fun foo(f: () -> Int): Int = f()