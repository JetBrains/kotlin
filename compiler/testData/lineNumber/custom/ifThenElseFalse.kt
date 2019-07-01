fun cond() = false

fun foo() {
    if (cond())
        cond()
    else
         false
}

// 1 4 5 7 8