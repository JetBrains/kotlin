// "Remove initializer from property" "true"
namespace a

import java.util.Collections

class M {
    trait A {
        abstract val l = <caret>Collections.emptyList<Int>()
    }
}