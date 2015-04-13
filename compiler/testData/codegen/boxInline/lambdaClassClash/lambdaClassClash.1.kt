//NO_CHECK_LAMBDA_INLINING
import zzz.*

fun box(): String {

    val p = { calc { 11 }} ()

    val z = { calc { 12 }}()

    if (p == z) return "fail"

    return "OK"
}
