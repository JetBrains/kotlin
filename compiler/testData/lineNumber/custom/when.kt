fun foo(x: Int) {
    when {
        x == 21 -> foo(x)
        x == 42 -> foo(x)
        else -> foo(x)
    }
    
    val t = when {
        x == 21 -> foo(x)
        x == 42 -> foo(x)
        else -> foo(x)
    }
}

// 1 3 4 5 9 10 11 8
