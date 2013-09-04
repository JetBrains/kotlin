fun f(x: Any): Int {
    return when (x)  {
        is Int ->1
        else->0
    }
}

// SET_FALSE: SPACE_AROUND_WHEN_ARROW