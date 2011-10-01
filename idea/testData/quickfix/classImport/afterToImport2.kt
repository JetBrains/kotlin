// "Remove initializer from property" "true"
namespace a

import java.util.Collections

namespace b {

import java.util.List

class M {
    trait A {
        abstract val l : <caret>List<Int>?
    }
}
}