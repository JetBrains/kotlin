fun main() {
    val bar: (Array<String>) -> Int = {<caret>(arr): Int -> arr.size}
}
