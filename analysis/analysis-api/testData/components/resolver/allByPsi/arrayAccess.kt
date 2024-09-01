fun usage(list: MutableList<Int>) {
    list += 1
    val implicitIndex = list[0]
    val explicitIndex = list.get(implicitIndex)
}

fun foo(i: Int, list: MutableList<Int>) {
    foo(--list[i], list)
}

// WITH_STDLIB
