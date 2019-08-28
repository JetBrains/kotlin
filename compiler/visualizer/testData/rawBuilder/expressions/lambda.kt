data class Tuple(val x: Int, val y: Int)

//                                  fun ((Tuple) -> Int).invoke(Tuple): Int
//                                  │ constructor Tuple(Int, Int)
//                                  │ │     Int   
//                                  │ │     │  Int
//                                  │ │     │  │  
inline fun use(f: (Tuple) -> Int) = f(Tuple(1, 2))

fun foo(): Int {
//      (Tuple) -> Int    
//      │                 
    val l1 = { t: Tuple ->
//              foo.<anonymous>.t: Tuple
//          Int │ val (Tuple).x: Int
//          │   │ │
        val x = t.x
//              foo.<anonymous>.t: Tuple
//          Int │ val (Tuple).y: Int
//          │   │ │
        val y = t.y
//      val foo.<anonymous>.x: Int
//      │ fun (Int).plus(Int): Int
//      │ │ val foo.<anonymous>.y: Int
//      │ │ │
        x + y
    }
//  fun use((Tuple) -> Int): Int
//  │               val foo.<anonymous>.x: Int
//  │      Int      │ fun (Int).plus(Int): Int
//  │      │  Int   │ │ val foo.<anonymous>.y: Int
//  │      │  │     │ │ │  
    use { (x, y) -> x + y }

//         fun use((Tuple) -> Int): Int
//         │    
    return use {
//      Unit                       
//      │   foo.<anonymous>.it: Tuple
//      │   │  val (Tuple).x: Int
//      │   │  │ fun (Any).equals(Any?): Boolean
//      │   │  │ │  Int           Int
//      │   │  │ │  │             │
        if (it.x == 0) return@foo 0
//                 foo.<anonymous>.it: Tuple
//                 │  val (Tuple).y: Int
//                 │  │
        return@use it.y
    }
}

fun bar(): Int {
//         fun use((Tuple) -> Int): Int
//         │           
    return use lambda@{
//      Unit                       
//      │   bar.<anonymous>.it: Tuple
//      │   │  val (Tuple).x: Int
//      │   │  │ fun (Any).equals(Any?): Boolean
//      │   │  │ │  Int           Int
//      │   │  │ │  │             │
        if (it.x == 0) return@bar 0
//                    bar.<anonymous>.it: Tuple
//                    │  val (Tuple).y: Int
//                    │  │
        return@lambda it.y
    }
}

//             collections/List<Int>
//             │
fun test(list: List<Int>) {
//      collections/MutableMap<Int, String>
//      │     fun <K, V> collections/mutableMapOf(): collections/MutableMap<Int, String>
//      │     │                          
    val map = mutableMapOf<Int, String>()
//  test.list: collections/List<Int>
//  │    fun <T> collections/Iterable<Int>.forEach((Int) -> Unit): Unit
//  │    │         val test.map: collections/MutableMap<Int, String>
//  │    │         │   fun <K, V> collections/MutableMap<Int, String>.getOrPut(Int, () -> String): String
//  │    │         │   │        test.<anonymous>.it: Int
//  │    │         │   │        │     fun <T> collections/mutableListOf(): collections/MutableList<???>
//  │    │         │   │        │     │                  fun (String).plus(Any?): String
//  │    │         │   │        │     │                  │      
    list.forEach { map.getOrPut(it, { mutableListOf() }) += "" }
}

//  () -> Unit  
//  │           
val simple = { }

//  () -> Int   Int 
//  │           │   
val another = { 42 }