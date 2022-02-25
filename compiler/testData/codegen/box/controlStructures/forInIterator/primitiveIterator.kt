// TARGET_BACKEND: JVM
// WITH_STDLIB

fun myFun(iterator: MyLongIterator) {
    for (x in iterator) {
        assert(x == 42L)
    }
}

class MyLongIterator : Iterator<Long> {
    private var count = 1
    override fun hasNext(): Boolean = count-- > 0
    override fun next(): Long = 42L
}

fun box(): String {
    myFun(MyLongIterator())
    return "OK"
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/util/Iterator.next
// 2 MyLongIterator.next
