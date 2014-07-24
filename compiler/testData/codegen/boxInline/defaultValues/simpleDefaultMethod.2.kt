package test

inline fun emptyFun(arg: String = "O") {

}

inline fun simpleFun(arg: String = "O"): String {
    val r = arg;
    return r;
}

