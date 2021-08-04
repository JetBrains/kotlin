package d

<!WRONG_MODIFIER_TARGET!>override<!> val f : ()-> Int = { 12 }

fun test() {
    f()
}

var g: Int = 1
    protected set(i: Int) {}
