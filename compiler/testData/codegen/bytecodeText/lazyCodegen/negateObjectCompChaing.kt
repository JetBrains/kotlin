val p: Int? = 1;
val z: Int? = 2;

fun test3() {
    if (!!!(p!! < z!!)) {
        val p = 1
    }
}
// 2 checkNotNull \(Ljava/lang/Object;\)V
// 1 IF_ICMP
