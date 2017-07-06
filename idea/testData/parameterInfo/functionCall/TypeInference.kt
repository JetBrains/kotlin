fun <A,B> foo(a: A, b: B): B = b
fun test(x: Any?) = foo(x, x!!<caret>).hashCode()

//Text: (a: Any?, <highlight>b: Any</highlight>), Disabled: false, Strikeout: false, Green: true