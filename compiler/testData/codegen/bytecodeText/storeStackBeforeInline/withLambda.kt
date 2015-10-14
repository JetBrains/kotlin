inline fun bar(x: String, block: (String) -> String) = "def" + block(x)
fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar("abc", bar("ghi") { x -> x + "jkl" }, "mno")
}

// 6 ASTORE
// 16 ALOAD
// 1 MAXLOCALS = 7
// 0 InlineMarker
