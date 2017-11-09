package kt17144

fun main(args: Array<String>) {
    val a = true
    1 + when (a) {
        true -> {
            println("foo")
            //Breakpoint!
            1
        }
        else -> {
            println("bar")
            2
        }
    }
}

//RESUME: 1