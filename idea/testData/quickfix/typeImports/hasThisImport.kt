// "Remove initializer from property" "true"
package a

import java.util.Collections
import java.util.List

class M {
    interface A {
        val l = <caret>Collections.emptyList<Int>()
    }
}