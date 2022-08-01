
class C {
    val length = 54
}

fun test(): Int {
    return (<expr>C</expr>::length).get(C())
}