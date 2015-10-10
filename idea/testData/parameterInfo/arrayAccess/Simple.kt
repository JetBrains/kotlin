class C {
    operator fun get(pInt: Int, pString: String){}
}

fun foo(c: C) {
    c[<caret>]
}

//Text: (<highlight>pInt: Int</highlight>, pString: String), Disabled: false, Strikeout: false, Green: true