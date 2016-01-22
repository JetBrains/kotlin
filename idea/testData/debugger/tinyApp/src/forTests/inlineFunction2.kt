package inlineFunctionOtherPackage

inline fun myFun(f: () -> Int): Int = f()

val String.prop: String
    get() {
        return "a"
    }