fun foo(a:Int):Int = a
fun bar(a:Int):Int = a

fun sumFooBar(a:Int, b:Int):Int = foo(a) + bar(b)

fun main(args:Array<String>) {
    if (sumFooBar(2, 3) != 5) throw Error()
}