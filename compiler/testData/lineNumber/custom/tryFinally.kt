fun foo() {
    try {
        System.out?.println()
    } finally {
        System.out?.println()
    }
    
    val t = try {
        System.out?.println()
    } finally {
        System.out?.println()
    }
}

// 1 2 3 5 8 9 11 8
