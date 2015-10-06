fun <T> f(t1: T, t2: T){}

fun test() {
    f(<caret>)
}

// TYPE: "1"

/*
Text: (<highlight>t1: Int</highlight>, t2: Int), Disabled: false, Strikeout: false, Green: true
*/
