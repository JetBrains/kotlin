package outer.nested

import outer.f

fun g() {
}

fun main(args: Array<String>) {
    f { }
    g()
}
