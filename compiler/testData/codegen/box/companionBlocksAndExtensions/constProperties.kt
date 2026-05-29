// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        const val BLOCK_CONST = "BlockConst"
    }
}

companion const val C.EXT_CONST = "ExtConst"

fun box(): String {
    if (C.BLOCK_CONST != "BlockConst") return "FAIL: BLOCK_CONST=${C.BLOCK_CONST}"
    if (C.EXT_CONST != "ExtConst") return "FAIL: EXT_CONST=${C.EXT_CONST}"

    return "OK"
}
