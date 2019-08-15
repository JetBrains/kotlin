package org.jetbrains.kotlin.test

val listOfInt = listOf(1, 2, 3)
val javaList = java.util.ArrayList<Int>()

fun move(): java.util.ArrayList<Int> {
    for (elem in listOfInt) {
        javaList.add(elem)
    }

    return javaList
}
