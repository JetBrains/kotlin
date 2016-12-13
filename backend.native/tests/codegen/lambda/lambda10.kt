fun main(args : Array<String>) {
    var str = "original"

    val lambda = {
        println(str)
    }

    lambda()

    str = "changed"
    lambda()
}