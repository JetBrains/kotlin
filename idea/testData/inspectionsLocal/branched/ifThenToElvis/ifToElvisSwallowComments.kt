fun foo(arg: Any): Int {
    // 1
    return <caret>if (arg is Int) arg
    // 2
    else 10
}