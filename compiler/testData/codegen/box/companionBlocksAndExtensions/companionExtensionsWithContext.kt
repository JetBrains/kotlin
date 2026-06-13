// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
class A
class C

context(_: A)
companion fun C.func(s: String) = s

context(_: A)
companion val C.readonly
    get() = "O"

context(_: A)
companion fun C.getOk(): String {
    return readonly + "K"
}

fun box(): String {
    return with(A()) {
        C.func(C.getOk())
    }
}
