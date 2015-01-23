fun foo() {
    return
    // val xyz has empty live range because everything after return will be removed as dead
    val xyz = 1
}

// 0 xyz
