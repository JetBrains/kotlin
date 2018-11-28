package m1

inline fun foo(action: () -> Int): Int {
    return action()
}

