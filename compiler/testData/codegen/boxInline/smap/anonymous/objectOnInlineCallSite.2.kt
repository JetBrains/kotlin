package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}

//SMAP ABSENT