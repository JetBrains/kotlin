fun foo() {
//            Int
//            │fun (Int).rangeTo(Int): ranges/IntRange
//       Int  ││ Int
//       │    ││ │
    for (i in 1..10) {
//      fun io/println(Int): Unit
//      │       val foo.i: Int
//      │       │
        println(i)
    }
}

fun fooLabeled() {
//  fun io/println(Any?): Unit
//  │
    println("!!!")
//                   Int
//                   │fun (Int).rangeTo(Int): ranges/IntRange
//              Int  ││ Int
//              │    ││ │
    label@ for (i in 1..10) {
//      Unit
//      │   val fooLabeled.i: Int
//      │   │ EQ operator call
//      │   │ │  Int
//      │   │ │  │
        if (i == 5) continue@label
//      fun io/println(Int): Unit
//      │       val fooLabeled.i: Int
//      │       │
        println(i)
    }
//  fun io/println(Any?): Unit
//  │
    println("!!!")
}

//            collections/List<String>
//            │
fun bar(list: List<String>) {
//                  bar.list: collections/List<String>
//                  │    fun (collections/List<String>).subList(Int, Int): collections/List<String>
//                  │    │       Int
//       String     │    │       │  Int
//       │          │    │       │  │
    for (element in list.subList(0, 10)) {
//      fun io/println(Any?): Unit
//      │       val bar.element: String
//      │       │
        println(element)
    }
//                  bar.list: collections/List<String>
//                  │    fun (collections/List<String>).subList(Int, Int): collections/List<String>
//                  │    │                fun io/println(Any?): Unit
//       String     │    │       Int Int  │       val bar.element: String
//       │          │    │       │   │    │       │
    for (element in list.subList(10, 20)) println(element)
}

data class Some(val x: Int, val y: Int)

//           collections/Set<Some>
//           │
fun baz(set: Set<Some>) {
//        Int
//        │  Int   baz.set: collections/Set<Some>
//        │  │     │
    for ((x, y) in set) {
//      fun io/println(Any?): Unit
//      │             val baz.x: Int
//      │             │      val baz.y: Int
//      │             │      │
        println("x = $x y = $y")
    }
}

//                      collections/List<Some>
//                      │
fun withParameter(list: List<Some>) {
//       Some       withParameter.list: collections/List<Some>
//       │          │
    for (s: Some in list) {
//      fun io/println(Any?): Unit
//      │       val withParameter.s: Some
//      │       │
        println(s)
    }
}
