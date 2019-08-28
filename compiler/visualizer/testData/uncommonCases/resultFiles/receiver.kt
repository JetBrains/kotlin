fun Int.addOne(): Int {
//              fun (Int).plus(Int): Int
//              │ Int
//              │ │
    return this + 1
}

//      Int
//      │
val Int.repeat: Int
    get() = this

fun main() {
//      Int Int
//      │   │
    val i = 2
//  val main.i: Int
//  │ fun Int.addOne(): Int
//  │ │
    i.addOne()
//          val main.i: Int
//          │ val Int.repeat: Int
//          │ │      fun (Int).times(Int): Int
//      Int │ │      │ Int
//      │   │ │      │ │
    val p = i.repeat * 2
}