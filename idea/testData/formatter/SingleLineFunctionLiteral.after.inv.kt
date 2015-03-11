fun test(some: (Int) -> Int) {
}

fun foo() = test() {it}
val function = test {(a: Int) -> a}
val function1 = test {a: Int -> a}
val function2 = test { }
val function3 = test {}
val function4 = test { }
val function5 = test {
}

// SET_TRUE: INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD