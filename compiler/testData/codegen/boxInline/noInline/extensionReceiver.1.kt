//NO_CHECK_LAMBDA_INLINING

fun box(): String {
    val res = { "OK" }.test()()
    if (res != "OKOKOK") return "fail 1: $res"

    val res2 = { "OK" }.extensionNoInline().subSequence(0, 2)
    if (res2 != "OK") return "fail 2: $res2"

    return "OK"
}
