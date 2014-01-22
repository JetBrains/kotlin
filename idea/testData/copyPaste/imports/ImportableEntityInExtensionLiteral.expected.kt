package to

import a.f
import a.A.Nested
import a.ext

fun g() {
    f {
        Inner()
        Nested()
        foo()
        ext()
    }
}