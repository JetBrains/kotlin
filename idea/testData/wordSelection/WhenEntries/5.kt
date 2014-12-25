fun main(args : Array<String>) {
    for (i in 1..100) {
        when {
<selection>            i%3 == 0 -> {print("Fizz"); continue;}
            i%5 == 0 -> {print("Buzz"); continue;}

            (i%3 != 0 && i%5 != 0) -> {print(i); continue;}<caret>
            else -> println()
</selection>        }
    }
}

fun foo() : Unit {
    println() {
        println()


        println()
        println()
    }

    println(array(1, 2, 3))
    println()
}