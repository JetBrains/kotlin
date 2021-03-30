// WITH_RUNTIME
fun simple() {
//      Int Int
//      │   │
    var x = 10
//  var simple.x: Int
//  │ fun (Int).plus(Int): Int
//  │ │  Int
//  │ │  │
    x += 20
//  var simple.x: Int
//  │ fun (Int).minus(Int): Int
//  │ │  Int
//  │ │  │
    x -= 5
//  var simple.x: Int
//  │ fun (Int).div(Int): Int
//  │ │  Int
//  │ │  │
    x /= 5
//  var simple.x: Int
//  │ fun (Int).times(Int): Int
//  │ │  Int
//  │ │  │
    x *= 10
}

//  collections/List<String>
//  │
fun List<String>.modify() {
//       fun <T> collections/Collection<T>.plus<String>(T): collections/List<T>
//       │
    this += "Alpha"
//       fun <T> collections/Collection<T>.plus<String>(T): collections/List<T>
//       │
    this += "Omega"
}

fun Any.modify() {
//           collections/List<Int>
//           │          fun <T> collections/Collection<T>.plus<Int>(T): collections/List<T>
//           │          │  Int
//           │          │  │
    (this as List<Int>) += 42
}
