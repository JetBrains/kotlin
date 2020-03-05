// FIR_IGNORE
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
//       fun <T> collections/Collection<String>.plus<String>(String): collections/List<String>
//       │
    this += "Alpha"
//       fun <T> collections/Collection<String>.plus<String>(String): collections/List<String>
//       │
    this += "Omega"
}

fun Any.modify() {
//           collections/List<Int>
//           │          fun <T> collections/Collection<Int>.plus<Int>(Int): collections/List<Int>
//           │          │  Int
//           │          │  │
    (this as List<Int>) += 42
}
