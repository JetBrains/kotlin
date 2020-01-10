// SET_TRUE: ALLOW_TRAILING_COMMA

val x: (Pair<Int, Int>, Int) -> Unit = { (x, y), z ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = { (x, y), z ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (x,
            y),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (
            x,
            y),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (
            x, // adw
            y,
    ),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (
            x, /* val x: (Pair<Int, Int>, Int) -> Unit = { (x, y), z, ->
    println(x)
}*/
    ),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = { (x, y), z ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = { (x, y/**/), z ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (x, y/*
*/),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (/**/x /**/  /*
*/, // awdawd
            y/*
*/),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (/**/x /**/  /*
*/, // awdawd
            y/*
*/),
    z,
    ->
    println(x)
}

val x: (Pair<Int, Int>, Int) -> Unit = {
    (x, y),
    z,
    ->
    println(x)
}