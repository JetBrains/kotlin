class X {
    companion object {
        fun instance(): X = X()
    }
}

fun f(insp: X){}

fun test(insa: Any) {
    f(ins<caret>)
}

// ORDER: instance
// ORDER: insp
// ORDER: insa
