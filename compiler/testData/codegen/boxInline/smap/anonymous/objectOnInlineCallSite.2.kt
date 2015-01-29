package builders

inline fun call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) init: () -> Unit) {
    return init()
}

//SMAP ABSENT