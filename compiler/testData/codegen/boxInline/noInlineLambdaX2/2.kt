var s: Int = 1;

inline fun Int.inlineMethod() : Int {
    noInlineLambda()
    return noInlineLambda()
}

inline fun Int.noInlineLambda() =  { s++ } ()
