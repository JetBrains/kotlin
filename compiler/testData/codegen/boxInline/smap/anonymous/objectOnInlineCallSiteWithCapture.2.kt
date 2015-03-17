package builders
//TODO there is a bug in asm it's skips linenumber on same line on reading bytecode
inline fun call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) init: () -> Unit) {
    "1"; return init()
}

inline fun test(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) p: () -> String): String {
    var res = "Fail"

    call {
        object {
            fun run () {
                res = p()
            }
        }.run()
    }

    return res
}
//TODO SHOULD BE LESS

