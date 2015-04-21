package inlineFun1

inline fun Int.myFun1(f: () -> Int): Int = f()