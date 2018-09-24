package m3


fun foofaa(): Unit { }

inline fun foo(action: () -> Int): Int {
    foofaa()
    return action()
}

