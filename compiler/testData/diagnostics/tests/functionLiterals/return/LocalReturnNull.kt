class A
val flag = true

val a /*: () -> A?*/ = l@ {
    if (flag) return@l null

    A()
}

val b /*: () -> A?*/ = l@ {
    if (flag) return@l null

    return@l A()
}