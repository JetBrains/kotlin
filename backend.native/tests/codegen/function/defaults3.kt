fun foo(a:Int = 2, b:String = "Hello", c:Int = 4):String = "$b-$c$a"
fun foo(a:Int = 3, b:Int = a + 1, c:Int = a + b) = a + b + c

fun main(arg:Array<String>){
    val a = foo(b="Universe")
    if (a != "Universe-42")
        throw Error()

    val b = foo(b = 5)
    if (b != (/* a = */ 3 + /* b = */ 5 + /* c = */ (3 + 5)))
        throw Error()
}