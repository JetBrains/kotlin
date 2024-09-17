// CHECK_BYTECODE_TEXT
// 0 invoke\(

fun box() =
    { s: String ->
        var ok = "O"
        ok += s
        ok
    }.invoke("K")
