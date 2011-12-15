// "Import Class" "true"
namespace a

import a.b.M

fun test() {
    val v = M
}

namespace b {
    class M() { }
}