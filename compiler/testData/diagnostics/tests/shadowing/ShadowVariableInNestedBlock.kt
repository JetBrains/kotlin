fun ff(): Int {
    var i = 1
    <!UNUSED_FUNCTION_LITERAL!>{
        val <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 2
    }<!>
    return i
}