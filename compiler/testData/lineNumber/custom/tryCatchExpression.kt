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

// 2 3 4 5 6 +8 9 10 11 8 13