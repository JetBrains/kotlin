package org.jetbrains.kotlin.test

//  collections/List<Int>
//  │           fun <T> collections/listOf(vararg Int): collections/List<Int>
//  │           │      Int
//  │           │      │  Int
//  │           │      │  │  Int
//  │           │      │  │  │
val listOfInt = listOf(1, 2, 3)
//  java/util/ArrayList<Int>
//  │          package java
//  │          │         constructor util/ArrayList<E : Any!>()
//  │          │         │
val javaList = java.util.ArrayList<Int>()

//          package java
//          │    package java/util
//          │    │
fun move(): java.util.ArrayList<Int> {
//               val listOfInt: collections/List<Int>
//               │
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
