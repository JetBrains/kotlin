class MyList

fun acceptList(l: MyList) { }

fun test() {
    acceptList(<expr>["1", "2", "3"]</expr>)
}

// LANGUAGE: +CollectionLiterals
// COMPILATION_ERRORS