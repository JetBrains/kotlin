// ISSUE: KT-68834
class Foo {
    var alias: Foo = this

    operator fun get(index: Int): Foo { return this }
    operator fun set(index: Int, arg: Foo) {  }
    operator fun plusAssign(arg: String) {  }
    operator fun inc(): Foo { return this }
    operator fun invoke(arg: String) {  }
}

operator fun Foo?.get(index: Int): Foo? { return this }
operator fun Foo?.set(index: Int, arg: Foo?) {}
operator fun Foo?.plusAssign(arg: String) {}
operator fun Foo?.inc(): Foo { return this!! }
operator fun Foo?.invoke(arg: String) { }

fun huh(arg: Foo?) {
    arg?.alias[42] 
    arg?.alias[42] = arg
    arg?.alias += "" 
    arg?.alias++ 
    ++arg?.alias 
    arg?.alias("") 
    arg?.alias[42] += ""
    arg?.alias[42]++
    ++arg?.alias[42]

    (arg?.alias)[42] 
    (arg?.alias)[42] = arg
    (arg?.alias) += "" 
    (arg?.<!VARIABLE_EXPECTED!>alias<!>)++
    ++(arg?.<!VARIABLE_EXPECTED!>alias<!>)
    (arg?.alias)("") 
    (arg?.alias)[42] += ""
    (arg?.alias[42]) += ""
    (arg?.alias[42])++
    ++(arg?.alias[42])
}
