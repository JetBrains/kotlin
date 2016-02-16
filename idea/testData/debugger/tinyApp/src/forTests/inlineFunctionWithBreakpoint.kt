package inlineFunctionWithBreakpoint

inline fun myFun(f: (Int) -> Unit) {
    f(1)
}