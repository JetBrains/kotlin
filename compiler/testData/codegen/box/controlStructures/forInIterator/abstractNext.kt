// TARGET_BACKEND: JVM
// WITH_STDLIB

fun iterate(iterator: MyIterator): String {
    for (x in iterator) return x
    return "Fail"
}

abstract class MyIterator : Iterator<String> {
    override fun hasNext(): Boolean = true
    abstract override fun next(): String
}

class MyIteratorImpl : MyIterator() {
    override fun next(): String = "OK"
}

fun box(): String = iterate(MyIteratorImpl())

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/util/Iterator.next
// 1 MyIterator.next
// 1 MyIteratorImpl.next
