package foo

class Foo {
    val a: Int

    fun foo(p: String) {
        val x = 1.0f
        <expr>print(x)</expr>
    }
}

class Unrelated {
    val unrelatedMember: Boolean
        get() = true
}

fun unrelatedFunction(): Int {
    return "foo".length
}