// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
import C.func

class C {
    companion {
        fun func(s: String) = s
        val readonly = "O"
        var mutable = ""

        fun getOk(): String {
            mutable = "K"
            return readonly + mutable
        }
    }
}

fun box(): String {
    return func(C.getOk())
}
