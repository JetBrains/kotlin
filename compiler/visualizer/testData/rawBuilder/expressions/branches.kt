// FIR_IGNORE
// WITH_STDLIB
//                            foo.a: Int
//                            │ fun (Int).compareTo(Int): Int
//                            │ │ foo.b: Int
//                      Int   │ │ │  foo.a: Int
//                      │ Int │ │ │  │      foo.b: Int
//                      │ │   │ │ │  │      │
fun foo(a: Int, b: Int) = if (a > b) a else b

fun bar(a: Double, b: Double): Double {
//  Nothing
//  │   bar.a: Double
//  │   │ fun (Double).compareTo(Double): Int
//  │   │ │ bar.b: Double
//  │   │ │ │
    if (a > b) {
//      fun io/println(Double): Unit
//      │       bar.a: Double
//      │       │
        println(a)
//             bar.a: Double
//             │
        return a
    } else {
//      fun io/println(Double): Unit
//      │       bar.b: Double
//      │       │
        println(b)
//             bar.b: Double
//             │
        return b
    }
}

fun baz(a: Long, b: Long): Long {
//  Nothing
//  │
    when {
//      baz.a: Long
//      │ fun (Long).compareTo(Long): Int
//      │ │ baz.b: Long
//      │ │ │    Nothing
//      │ │ │    │
        a > b -> {
//          fun io/println(Long): Unit
//          │       baz.a: Long
//          │       │
            println(a)
//                 baz.a: Long
//                 │
            return a
        }
//              Nothing
//              │      baz.b: Long
//              │      │
        else -> return b
    }
}

fun grade(g: Int): String {
//         String
//         │     grade.g: Int
//         │     │
    return when (g) {
//      Int
//      │  Int  String
//      │  │    │
        6, 7 -> "Outstanding"
//      Int  String
//      │    │
        5 -> "Excellent"
//      Int  String
//      │    │
        4 -> "Good"
//      Int  String
//      │    │
        3 -> "Mediocre"
//      fun (ranges/IntRange).contains(Int): Boolean
//      │  Int
//      │  │fun (Int).rangeTo(Int): ranges/IntRange
//      │  ││ Int  String
//      │  ││ │    │
        in 1..2 -> "Fail"
//                   String
//                   │
        is Number -> "Number"
//              String
//              │
        else -> "Unknown"
    }
}
