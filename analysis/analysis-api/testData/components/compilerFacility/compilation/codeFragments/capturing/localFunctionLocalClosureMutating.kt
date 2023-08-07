fun test() {
    var x = 0

    fun call() {
        x = 1
    }

    <caret>call()
}