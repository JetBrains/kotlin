// "Change type of 'complex' to '(Int) -> Long'" "true"

val complex: (Int) -> String
    get() = { it.toLong()<caret> }
/* IGNORE_FIR */
