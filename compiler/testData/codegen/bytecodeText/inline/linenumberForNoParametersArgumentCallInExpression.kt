fun box() {
    lookAtMe {
        12
    }
}

inline fun lookAtMe(f: () -> Int): Int {
    val a = 42
    a + f() // Even this line already has meaningful instraction nop is still generated
    return 13
}

// TODO: Less NOPs is better
// 1 NOP