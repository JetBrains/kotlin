// ISSUE: KT-75949: Inline function is not found: box.serialization.deserializedMembers_kt75949.foo.bar/C.D.foo|foo(){}[0]
// MODULE: lib
// FILE: lib.kt
package foo.bar

var counter: Int = 0

class C {
    inline fun foo() {
        counter += 2
    }
    class D {
        inline fun foo() {
            counter += 40
        }
    }
}
// MODULE: main(lib)
// FILE: main.kt
import foo.bar.*

fun box(): String {
    C().foo()
    C.D().foo()

    return if (counter == 42) "OK"
    else counter.toString()
}

