// "Remove initializer from property" "true"
namespace a

import java.util.Collections

namespace b {

class M {
    trait A {
        abstract val l = <caret>Collections.emptyList<Int>()
    }
}
}