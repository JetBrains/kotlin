// ISSUE: KT-52580

interface Base {
    val a: String
}

interface Derived : Base {
    override val a: String
    val b: Int
}

class BaseImpl(override val a: String) : Base

fun test(base: Base): String {
    return consume(
        base.run { a },
        (base as? Derived)?.b?.toString(),
        base.a
    )
}

fun consume(s1: String, s2: String?, s3: String): String {
    return "$s1|$s2|$s3"
}

fun box(): String {
    val result = test(BaseImpl("Base"))
    return when (result) {
        "Base|null|Base" -> "OK"
        else -> "Fail: $result"
    }
}
