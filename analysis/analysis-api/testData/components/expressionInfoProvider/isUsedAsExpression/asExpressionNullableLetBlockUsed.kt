fun test(v: Any?) {
    val x = (v as? String)?.let <expr>{
        it.length
    }</expr>
}