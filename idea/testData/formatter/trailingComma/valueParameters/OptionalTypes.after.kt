val foo1: (Int, Int) -> Int = fun(
        x,
        y,
): Int = 42

val foo2: (Int, Int) -> Int = fun(
        x,
        y,
): Int {
    return x + y
}

val foo3: (Int, Int) -> Int = fun(
        x, y,
): Int {
    return x + y
}

val foo4: (Int) -> Int = fun(
        x,
): Int = 42

val foo5: (Int) -> Int = fun(
        x,
): Int = 42

val foo6: (Int) -> Int = fun(
        x,
): Int = 42

val foo7: (Int) -> Int = fun(x): Int = 42

val foo8: (Int, Int, Int) -> Int = fun(
        x, y: Int, z,
): Int {
    return x + y
}

val foo9: (Int, Int, Int) -> Int = fun(
        x,
        y: Int,
        z,
): Int = 42

val foo10: (Int, Int, Int) -> Int = fun(
        x,
        y: Int,
        z: Int,
): Int = 43

val foo10 = fun(
        x: Int,
        y: Int,
        z: Int,
): Int = 43

val foo11 = fun(
        x: Int,
        y: Int,
        z: Int,
): Int = 43

val foo12 = fun(
        x: Int, y: Int, z: Int,
): Int = 43

val foo13 = fun(
        x: Int, y: Int, z: Int,
): Int = 43

val foo14 = fun(
        x: Int, y: Int, z: Int,
): Int = 43

// SET_TRUE: ALLOW_TRAILING_COMMA