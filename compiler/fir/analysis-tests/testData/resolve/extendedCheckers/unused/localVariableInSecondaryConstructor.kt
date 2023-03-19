fun main() {

    val localVariable = 0

    class LocalClass(val arg: Int) {
        constructor() : this(localVariable)
    }

    LocalClass().arg
}
