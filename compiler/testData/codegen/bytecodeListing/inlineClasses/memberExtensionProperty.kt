inline class Z1(val s: Int) {
    val String.ext: Int get() = 239
}

inline class Z2(val s: Int) {
    val String.s: Int get() = 239
}

interface StrS {
    val String.s: Int
}

inline class Z3(val s: Int) : StrS {
    override val String.s: Int get() = 239
}