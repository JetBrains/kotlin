fun sumIB(a:Int, b:Byte  ) = a + b
fun sumIS(a:Int, b:Short ) = a + b
fun sumII(a:Int, b:Int   ) = a + b
fun sumIL(a:Int, b:Long  ) = a + b
fun sumIF(a:Int, b:Float ) = a + b
fun sumID(a:Int, b:Double) = a + b

fun modID(a:Int, b:Double) = a % b

fun main(args:Array<String>) {
    if (sumIB(2, 3)    != 5)    throw Error()
    if (sumIS(2, 3)    != 5)    throw Error()
    if (sumII(2, 3)    != 5)    throw Error()
    if (sumIL(2, 3L)   != 5L)   throw Error()
    if (sumIF(2, 3.0f) != 5.0f) throw Error()
    if (sumID(2, 3.0)  != 5.0)  throw Error()
    if (modID(5, 3.0)  != 2.0)  throw Error()
}