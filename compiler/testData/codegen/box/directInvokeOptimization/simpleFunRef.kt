// CHECK_BYTECODE_TEXT
// 0 invoke\(

fun ok(s: String) = "O" + s

fun box() =
    ::ok.invoke("K")
