val flag = true

val a/*: () -> Comparable<out Any?>*/ = l@ {
    return@l if (flag) "OK" else 4
}