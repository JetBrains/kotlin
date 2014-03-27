fun main() {
    val oom: (Int)->Int = {<caret>
        it * 2
    }
}