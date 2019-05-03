fun test() {
    if (false)<caret>
        while (true)
            foo(1)
    //comment about foo(2)
    foo(2)
}

fun foo(i: Int) {}