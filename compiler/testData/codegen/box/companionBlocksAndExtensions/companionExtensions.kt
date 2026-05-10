// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
class C

companion fun C.func(s: String) = s
companion val C.readonly = "O"
companion var C.mutable = ""

companion fun C.getOk(): String {
    mutable = "K"
    return readonly + mutable
}

fun box(): String {
    return C.func(C.getOk())
}
