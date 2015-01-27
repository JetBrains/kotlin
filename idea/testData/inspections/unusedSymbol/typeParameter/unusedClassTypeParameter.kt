class UnusedClassTypeParameter<T>(p: String) {
    {
        println(p)
    }
}

fun main(args: Array<String>) {
    UnusedClassTypeParameter("")
}