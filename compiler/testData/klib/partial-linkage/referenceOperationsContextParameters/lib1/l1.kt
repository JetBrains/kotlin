context(c1: String, c2: Char)
fun removedCtxFun(x: Int): String = "removedCtxFun($c1, $c2, $x)"

object ObjectWithRemovedCtxFun {
    context(c: String)
    fun removedMemberCtxFun(x: Int): String = "removedMemberCtxFun($c, $x)"
}

context(c: String)
val removedCtxVal: String
    get() = "removedCtxVal($c)"

private var removedCtxVarStorage = ""

context(c: String)
var removedCtxVar: String
    get() = removedCtxVarStorage
    set(v) { removedCtxVarStorage = "removedCtxVar($c, $v)" }

context(c: String)
fun funWithAddedCtx(x: Int): String = "funWithAddedCtx.v1($c, $x)"

context(c1: String, c2: Char)
fun funWithRemovedCtx(x: Int): String = "funWithRemovedCtx.v1($c1, $c2, $x)"

context(c: String)
fun funWithCtxTurnedIntoParam(x: Int): String = "funWithCtxTurnedIntoParam.v1($c, $x)"

fun funWithParamTurnedIntoCtx(c: String, x: Int): String = "funWithParamTurnedIntoCtx.v1($c, $x)"

context(c1: String, c2: Char)
fun funWithAllCtxRemoved(x: Int): String = "funWithAllCtxRemoved.v1($c1, $c2, $x)"

fun funWithCtxGained(x: Int): String = "funWithCtxGained.v1($x)"

context(c1: String, c2: Char)
fun funWithSwappedCtx(x: Int): String = "funWithSwappedCtx.v1($c1, $c2, $x)"

fun String.extFunMigratedToCtx(x: Int): String = "extFunMigratedToCtx.v1($this, $x)"

context(c: String)
fun funWithRenamedCtx(x: Int): String = "funWithRenamedCtx.v1($c, $x)"

context(c: String)
val valWithChangedCtxType: String
    get() = "valWithChangedCtxType.v1($c)"

context(c: String)
fun Int.removedCtxExtFun(x: Int): String = "removedCtxExtFun($c, $this, $x)"

class RemovedCtxClass

context(c1: String, c2: Char)
fun survivingCtxFun(x: Int): String = "survivingCtxFun($c1, $c2, $x)"

object ObjectWithSurvivingCtxFun {
    context(c: String)
    fun survivingMemberCtxFun(x: Int): String = "survivingMemberCtxFun($c, $x)"
}

context(c: String)
fun Int.survivingCtxExtFun(x: Int): String = "survivingCtxExtFun($c, $this, $x)"

private var survivingCtxVarStorage = ""

context(c: String)
var survivingCtxVar: String
    get() = survivingCtxVarStorage
    set(v) { survivingCtxVarStorage = "survivingCtxVar($c, $v)" }
