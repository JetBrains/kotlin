// "Remove initializer from property" "true"
package a

import java.util.Collections

class M {
    trait A {
        abstract val l: List<Int><caret>
    }
}