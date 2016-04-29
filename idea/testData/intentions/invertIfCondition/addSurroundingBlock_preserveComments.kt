fun foo(list: List<String>) {
    for (s in list)
        <caret>if (s.length > 0 /* check it */) /* then */ {
            bar() // bar()
        }
    }
}

fun bar(){}
