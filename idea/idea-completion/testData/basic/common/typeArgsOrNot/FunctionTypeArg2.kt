fun <T> genericFoo(p: Int){}
fun <T> genericFoo(c: Char){}

fun foo() {
    genericFoo<<caret>
}

// EXIST: String
// EXIST: java
// ABSENT: defaultBufferSize
// ABSENT: readLine
