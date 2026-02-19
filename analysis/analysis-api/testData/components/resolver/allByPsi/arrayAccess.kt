fun usage(list: MutableList<Int>) {
    list += 1
    val implicitIndex = list[0]
    val explicitIndex = list.get(implicitIndex)
    (@Suppress("SOME_ERROR") list[1]) += 2
    (list[2]) += 3
}

fun foo(i: Int, list: MutableList<Int>) {
    foo(--list[i], list)
    ++(list[2])
}

// WITH_STDLIB
// IGNORE_STABILITY_K2: symbol
// ^ The suppress is about parenthesized array accesses. Not a big problem since the code is incorrect anyway