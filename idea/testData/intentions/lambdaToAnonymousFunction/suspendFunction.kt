// IS_APPLICABLE: false
suspend fun sus(i: Int, block: suspend (Int) -> Unit) {
    block(i)
}

suspend fun main() {
    sus(1) {<caret>
    }
}