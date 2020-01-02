inline fun inlineFun(s: (p: Int) -> Unit) : (p: Int) -> Unit {
    return s
}

inline fun inlineFun2(s: (p: Int) -> Unit) : (p: Int) -> Unit = s


inline fun inlineFunWithExt(ext: Int.(p: Int) -> Unit) : Int.(p: Int) -> Unit {
    return ext
}

inline fun inlineFunWithExt2(ext: Int.(p: Int) -> Unit) : Int.(p: Int) -> Unit = ext



inline fun Function1<Int, Unit>.inlineExt(): Function1<Int, Unit> {
    return this
}

inline fun Function1<Int, Unit>.inlineExt2(): Function1<Int, Unit> = this