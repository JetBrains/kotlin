// TARGET_BACKEND: JVM
// WITH_STDLIB

fun iterate(iterator: MyIterator): String {
    for (x in iterator) return x
    return "Fail"
}

class MyIterator : Iterator<String> {
    @JvmName("unrelated")
    private fun String.next(): String = throw AssertionError("Should not be called")

    override fun hasNext(): Boolean = true
    override fun next(): String = "OK"
}

fun box(): String = iterate(MyIterator())

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/util/Iterator.next
// 2 MyIterator.next
