// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION +UNUSED_VARIABLE
fun ff(): Int {
    var i = 1
    <!UNUSED_LAMBDA_EXPRESSION!>{
        val <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 2
    }<!>
    return i
}
