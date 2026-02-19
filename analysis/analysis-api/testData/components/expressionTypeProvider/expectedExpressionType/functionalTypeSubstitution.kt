fun <K> test(f: java.lang.Comparable<K>) {}

fun f() {
    test(java.lang.Comparable {<caret>
            s: String -> 0
    })
}
