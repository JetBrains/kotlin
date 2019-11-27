// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.collections.AbstractIterator

class MyIterator : AbstractIterator<String>() {
    var i = 0
    public override fun computeNext() {
        if(i < 5)
          setNext((i++).toString())
    }

}

fun box() : String {
    var k = ""
    for (x in MyIterator()) {
        k+=x
    }
    return if(k=="01234") "OK" else k
}
