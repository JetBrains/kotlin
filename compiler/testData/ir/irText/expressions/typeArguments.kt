// WITH_RUNTIME

fun test1(x: Any) =
        x is Array<*> && x.isArrayOf<String>()