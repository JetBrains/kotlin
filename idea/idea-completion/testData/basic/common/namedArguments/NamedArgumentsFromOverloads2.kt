fun foo(p: Int, xxP1: String, xxP2: Int){}
fun foo(p: Int, xxP1: String, xxP2: Char){}
fun foo(p: Int, xxx: Any?){}
fun foo(p: String, xxy: String){}

fun f() {
    foo(1, xx<caret>)
}

// EXIST: { itemText:"xxP1 =", tailText: " String" }
// EXIST: { itemText:"xxP2 =", tailText: " ..." }
// EXIST: { itemText:"xxx =", tailText: " Any?" }
// EXIST: { itemText:"xxx = null", tailText: null }
// NOTHING_ELSE
