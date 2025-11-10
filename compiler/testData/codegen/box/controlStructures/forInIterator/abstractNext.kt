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

// There are 2 `INVOKEVIRTUAL MyIterator.next ()Ljava/lang/String;` instructions:
// * one in the bridge 'MyIterator.next()Ljava/lang/Object'
// * one in the 'iterate' fun

// CHECK_BYTECODE_TEXT
// 0 java/util/Iterator.next
// 2 MyIterator.next
// 1 MyIteratorImpl.next
