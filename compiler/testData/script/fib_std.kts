// Expecting two string parameters

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    System.out.println("fib($n)=$v")
    return v
}

System.out.println("num: ${args[0]} (${args[1]})")
val result = fib(java.lang.Integer.parseInt(args[0]))
