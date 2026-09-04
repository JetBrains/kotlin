// WITH_STDLIB
fun box(): String {
    var scope: Any? = null
    val r = sequence { scope = this; yield(1) }.toList()
    return if (r == listOf(1) && scope != null) "OK" else "Fail: $r, $scope"
}
