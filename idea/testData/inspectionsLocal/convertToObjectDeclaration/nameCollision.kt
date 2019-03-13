import Main.NotMain

class <caret>Main {
    companion object NotMain {
        fun check() {}
    }
}

fun main() {
    class Main
    NotMain.check()
}