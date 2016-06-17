// "Change 'complex' type to '(Int) -> Long'" "true"

val complex: (Int) -> String
    get() = { it.toLong()<caret> }