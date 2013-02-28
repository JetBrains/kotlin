fun foo() {
    try {
        System.out?.println()
    } catch (e: Throwable) {
        return
    }
    
    val t = try {
        System.out?.println()
    } catch (e: Throwable) {
        return
    }
}

// 1 2 3 5 8 9 11 8
