// ERROR: Nested class 'Nested' should be qualified as 'A.Nested'
package to

import a.ext
import a.f

fun g() {
    f {
        Inner()
        Nested()
        foo()
        ext()
    }
}