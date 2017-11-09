// KT-14158: NoSuchElementException while compiling...

fun foo(): Int {
    when (true) {
        return 0 -> return 1
    }
}