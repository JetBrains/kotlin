// "Remove initializer from property" "true"
package a

import java.util.Collections
import java.util.List

class M {
    trait A {
        abstract val l : <caret>List<Int>?
    }
}