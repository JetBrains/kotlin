// ISSUE: KT-68834
// IGNORE_BACKEND_K1: ANY

var result = "none"

fun member(name: String) {
    result = "$name: member"
}

fun extension(name: String) {
    result = "$name: extension"
}

fun checkResult(s: String) {
    if (s != result) throw RuntimeException("fail: $s, but $result")
}

class Foo {
    var alias: Foo = this

    operator fun get(index: Int): Foo { member("get"); return this }
    operator fun set(index: Int, arg: Foo) { member("set") }
    operator fun plusAssign(arg: String) { member("plusAssign") }
    operator fun inc(): Foo { member("inc"); return this }
    operator fun invoke(arg: String) { member("invoke") }
}

operator fun Foo?.get(index: Int): Foo? { extension("get"); return this }
operator fun Foo?.set(index: Int, arg: Foo?) { extension("set") }
operator fun Foo?.plusAssign(arg: String) { extension("plusAssign") }
operator fun Foo?.inc(): Foo { extension("inc"); return this!! }
operator fun Foo?.invoke(arg: String) { extension("invoke") }

fun huh(arg: Foo?) {
    arg?.alias[42]
    checkResult("get: member")
    arg?.alias[42] = arg
    checkResult("set: member")
    arg?.alias += ""
    checkResult("plusAssign: member")
    arg?.alias++
    checkResult("inc: member")
    ++arg?.alias
    checkResult("inc: member")
    arg?.alias("")
    checkResult("invoke: member")
    arg?.alias[42] += ""
    checkResult("plusAssign: member")
    arg?.alias[42]++
    checkResult("set: member")
    ++arg?.alias[42]
    checkResult("get: member")

    (arg?.alias)[42]
    checkResult("get: member")
    (arg?.alias)[42] = arg
    checkResult("set: member")
    (arg?.alias) += ""
    checkResult("plusAssign: member")
    (arg?.alias)++
    checkResult("inc: member")
    ++(arg?.alias)
    checkResult("inc: member")
    (arg?.alias)("")
    checkResult("invoke: member")
    (arg?.alias)[42] += ""
    checkResult("plusAssign: member")
    (arg?.alias[42]) += ""
    checkResult("plusAssign: member")
    (arg?.alias[42])++
    checkResult("set: member")
    ++(arg?.alias[42])
    checkResult("get: member")
}

fun box(): String {
    huh(Foo())
    return "OK"
}
