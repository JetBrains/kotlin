fun doSomething<T>(a: T) {}

fun main(args: Array<String>) {
    var a: String? = "A"
    doSomething(a?.<caret>length)
}

