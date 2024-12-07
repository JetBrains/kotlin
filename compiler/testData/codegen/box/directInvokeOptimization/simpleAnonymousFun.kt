// CHECK_BYTECODE_TEXT
// 0 invoke\(

fun box() =
    (fun (s: String): String {
        var ok = "O"
        ok += s
        return ok
    }).invoke("K")
