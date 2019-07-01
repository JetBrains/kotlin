fun funWithoutArgs(): Int {
    return Any().hashCode().toInt()
}

fun funWithAnyArg(value_1: Any): Int {
    return value_1.hashCode()
}

fun <K> select(vararg x: K): K = x[0]

fun <K> expandInv(vararg x: Inv<K>): K = x[0] as K
fun <K> expandIn(vararg x: In<K>): K = x[0] as K
fun <K> expandOut(vararg x: Out<K>): K = x[0] as K

fun <K> expandInvWithRemoveNullable(vararg x: Inv<K?>): K = x[0] as K
fun <K> expandInWithRemoveNullable(vararg x: In<K?>): K = x[0] as K
fun <K> expandOutWithRemoveNullable(vararg x: Out<K?>): K = x[0] as K

fun <K> removeNullable(vararg x: K?): K = x as K

fun <T> T.funT() = 10
fun <T> T?.funNullableT = 10

fun Any.funAny() = 10
fun Any?.funNullableAny = 10


fun funNothingQuest() = null