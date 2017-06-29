package bar

import foo.parenB
import foo.parenBP
import foo.parenP
import foo.parenPB
import foo.parenPP

fun some2(p1: () -> Unit, p2: (() -> Unit) -> Unit) {
    parenB { p1() }
    parenP()
    parenBP { p1 }()
    parenPP()()
    (parenPB(p2)) {}
}