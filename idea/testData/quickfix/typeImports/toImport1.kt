// "Remove initializer from property" "true"
package a

import java.util.Collections

class M {
    interface A {
        abstract val l = <caret>Collections.emptyList<Int>()
    }
}