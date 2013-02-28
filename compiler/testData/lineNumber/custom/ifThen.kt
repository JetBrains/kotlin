fun foo() {
    if (0 < 1) {
        return
    }
    
    val u: Unit = if (0 < 1) {
        return
    }
}

// 1 2 3 6 7 6
