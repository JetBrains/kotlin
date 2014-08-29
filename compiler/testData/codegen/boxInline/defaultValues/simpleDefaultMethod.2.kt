package test

inline fun emptyFun(arg: String = "O") {

}

inline fun simpleFun(arg: String = "O"): String {
    val r = arg;
    return r;
}


inline fun simpleDoubleFun(arg: Double = 1.0): Double {
    val r = arg + 1;
    return r;
}

