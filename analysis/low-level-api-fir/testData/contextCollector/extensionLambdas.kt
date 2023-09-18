fun test() {
    block("foo") foo@{
        block(42) num@{
            <expr>consume(this@foo.length + this@num)</expr>
        }
    }
}

fun <T> block(reciever: T, block: T.() -> Unit) {
    receiver.block()
}

fun consume(num: Int) {}