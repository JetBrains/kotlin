fun foo() {
//      Int Int
//      │   │
    val x = 1
//          val foo.x: Int
//          │ fun (Int).plus(Int): Int
//      Int │ │ Int
//      │   │ │ │
    var y = x + 1
//          var foo.y: Int
//          │ fun (Int).times(Int): Int
//      Int │ │ Int
//      │   │ │ │
    val z = y * 2
//  var foo.y: Int
//  │   var foo.y: Int
//  │   │ fun (Int).plus(Int): Int
//  │   │ │ val foo.z: Int
//  │   │ │ │
    y = y + z
//          var foo.y: Int
//          │ fun (Int).minus(Int): Int
//      Int │ │ val foo.x: Int
//      │   │ │ │
    val w = y - x
//         val foo.w: Int
//         │
    return w
}
