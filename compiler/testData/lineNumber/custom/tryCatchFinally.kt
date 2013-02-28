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

// 1 2 3 7 5 7 10 11 15 13 15 10
