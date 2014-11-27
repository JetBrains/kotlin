val p: Int? = 1;
val z: Int? = 2;

fun test3() {
    if (!!!(p!! < z!!)) {
        val p = 1
    }
}
// 2 IFNONNULL
// 3 IF
