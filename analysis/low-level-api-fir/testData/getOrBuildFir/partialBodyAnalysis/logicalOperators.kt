fun test(flag: Boolean, another: Boolean) {
    consume(<expr>flag && another</expr>)
    consume(<expr>flag || another</expr>)
    consume(flag xor another)
}

fun consume(a: Boolean) {}