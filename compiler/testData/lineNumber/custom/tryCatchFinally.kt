fun foo() {
    try {
        System.out?.println()
    } catch (e: Throwable) {
        System.out?.println()
    } finally {
        System.out?.println()
    }
    
    val t = try {
        System.out?.println()
    } catch (e: Throwable) {
        System.out?.println()
    } finally {
        System.out?.println()
    }
}

// 2 3 7 8 4 5 7 8 7 8 +10 11 15 16 12 13 15 16 15 10 17