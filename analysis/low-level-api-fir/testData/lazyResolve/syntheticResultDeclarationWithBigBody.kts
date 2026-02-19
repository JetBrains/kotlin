// RESOLVE_SCRIPT
// MEMBER_NAME_FILTER: $$result

fun <T> foo(action: () -> T): T = action()
foo {
    println("foo")
    foo {
        val i = 1
        println(i)
    }
}
