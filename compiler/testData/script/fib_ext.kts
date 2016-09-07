
// this script expected parameter num : Int

fun fib(n: Int): Int {
    val v = if(n < 2) 1 else fib(n-1) + fib(n-2)
    System.out.println("fib($n)=$v")
    return v
}

val hdr = "Num".decapitalize()

org.junit.Assert.assertTrue(true)

System.out.println("$hdr: $num")
val result = fib(num)

