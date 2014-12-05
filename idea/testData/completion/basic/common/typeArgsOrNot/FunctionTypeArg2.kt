fun genericFoo<T>(p: Int){}
fun genericFoo<T>(c: Char){}

fun foo() {
    genericFoo<<caret>
}

// EXIST: String
// EXIST: java
// ABSENT: defaultBufferSize
// ABSENT: readLine
