// FIR_IGNORE
package org.jetbrains.kotlin.test

//  collections/List<Int>
//  │           fun <T> collections/listOf<Int>(vararg Int): collections/List<Int>
//  │           │      Int
//  │           │      │  Int
//  │           │      │  │  Int
//  │           │      │  │  │
val listOfInt = listOf(1, 2, 3)
//  java/util/ArrayList<Int>
//  │                    constructor java/util/ArrayList<E : Any!>()
//  │                    │
val javaList = java.util.ArrayList<Int>()

fun move(): java.util.ArrayList<Int> {
//       Int     val listOfInt: collections/List<Int>
//       │       │
    for (elem in listOfInt) {
//      val javaList: java/util/ArrayList<Int>
//      │        fun (java/util/ArrayList<Int>).add(Int): Boolean
//      │        │   val move.elem: Int
//      │        │   │
        javaList.add(elem)
    }

//         val javaList: java/util/ArrayList<Int>
//         │
    return javaList
}
