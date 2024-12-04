// ISSUE: KT-68796
fun foo() {
    arg?.alias
    arg?.alias++
    arg?.alias[42]
    arg?.alias[42]++
    arg?.alias("")
    arg?.alias("")++
    arg?.alias[42]("")
    arg?.alias[42]("")++

    arg?.bar()
    arg?.bar()++
    arg?.bar()[42]
    arg?.bar()[42]++
    arg?.bar()("")
    arg?.bar()("")++
    arg?.bar()[42]("")
    arg?.bar()[42]("")++

    arg?.baz()()()
    arg?.baz()()()++
    arg?.baz()()()[42]
    arg?.baz()()()[42]++
    arg?.baz()()()("")
    arg?.baz()()()("")++
    arg?.baz()()()[42]("")
    arg?.baz()()()[42]("")++
}
