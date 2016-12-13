fun main(args : Array<String>) {
    val lambda = { s1: String, s2: String ->
        println(s1)
        println(s2)
    }

    lambda("one", "two")
}