// "Remove initializer from property" "true"
package a

import java.util.Collections
import java.util.List

class M {
    trait A {
        val l: jet.List<Int><caret>
    }
}