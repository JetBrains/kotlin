fun foo(s: String){}
fun foo(c: Char){}

fun bar(b: Boolean, s: String, c: Char){
    foo(if (b) {
        println()
        <caret>
    })
}

// EXIST: s
// EXIST: c
// ABSENT: b
