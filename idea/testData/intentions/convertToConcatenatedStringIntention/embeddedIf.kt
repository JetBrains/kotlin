fun main(args: Array<String>) {
    val x = "<caret>${if (true) 42 else 12}abc${if (true) 12 else 42}"
}
