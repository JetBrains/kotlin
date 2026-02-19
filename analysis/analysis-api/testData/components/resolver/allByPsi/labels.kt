fun foo(action: () -> Unit) {
    action()
}

fun usage() {
    foo label@{
        return@label
    }

    myFor@ for (i in 0..1) {
        continue@myFor
    }
}