fun test(obj: Any) {
    if (<expr>obj</expr> !is String) return
    consume(<expr_1>obj</expr_1>)
    obj.toString()
}

fun <T : CharSequence> consume(obj: T) {}