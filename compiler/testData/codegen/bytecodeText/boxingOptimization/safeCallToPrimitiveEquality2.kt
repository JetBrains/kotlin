fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun doChain1(s: String?) = s?.drop2()?.length == 1

fun doChain2(s: String?) = 1 == s?.drop2()?.length

// 0 valueOf
