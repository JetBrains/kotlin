fun test() {
    consume(<expr>1 + 2</expr>)
    consume(<expr_1>"foo"</expr_1>)
    consume('b')
}

fun consume(a: Int) {}