
// this script expected parameter num : Int

@file:DependsOnTwo(path2 = "@{runtime}")

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    println("fib($n)=$v")
    return v
}

val hdr = "Num".decapitalize()

println("$hdr: $num")
val result = fib(num)

