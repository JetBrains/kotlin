// WITH_STDLIB
fun foo(limit: Int) {
//      Int Int
//      │   │
    var k = 0
//               var foo.k: Int
//               │ fun (Int).compareTo(Int): Int
//               │ │ foo.limit: Int
//               │ │ │
    some@ while (k < limit) {
//      var foo.k: Int
//      │fun (Int).inc(): Int
//      ││
        k++
//      fun io/println(Int): Unit
//      │       var foo.k: Int
//      │       │
        println(k)
//             var foo.k: Int
//             │ EQ operator call
//             │ │  Int
//             │ │  │
        while (k == 13) {
//          var foo.k: Int
//          │fun (Int).inc(): Int
//          ││
            k++
//          Unit
//          │   var foo.k: Int
//          │   │ fun (Int).compareTo(Int): Int
//          │   │ │ foo.limit: Int
//          │   │ │ │
            if (k < limit) break@some
//          Unit
//          │   var foo.k: Int
//          │   │ fun (Int).compareTo(Int): Int
//          │   │ │ foo.limit: Int
//          │   │ │ │
            if (k > limit) continue
        }
    }
}

fun bar(limit: Int) {
//      Int bar.limit: Int
//      │   │
    var k = limit
    do {
//      var bar.k: Int
//      │fun (Int).dec(): Int
//      ││
        k--
//      fun io/println(Int): Unit
//      │       var bar.k: Int
//      │       │
        println(k)
//           var bar.k: Int
//           │ fun (Int).compareTo(Int): Int
//           │ │  Int
//           │ │  │
    } while (k >= 0)
}
