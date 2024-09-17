// CHECK_BYTECODE_TEXT
// 0 invoke\(

fun box() =
    { k: String ->
        { o: String ->
            var ok = o
            ok += k
            ok
        }("O")
    }("K")
