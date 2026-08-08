context(c1: String, c2: Char)
fun removedCtxFun(x: Int): String = "removedCtxFun($c1, $c2, $x)"

context(c: String)
val removedCtxVal: String
    get() = "removedCtxVal($c)"

private var removedCtxVarStorage = ""

context(c: String)
var removedCtxVar: String
    get() = removedCtxVarStorage
    set(v) { removedCtxVarStorage = "removedCtxVar($c, $v)" }

context(c1: String, c2: Char)
fun survivingCtxFun(x: Int): String = "survivingCtxFun($c1, $c2, $x)"
