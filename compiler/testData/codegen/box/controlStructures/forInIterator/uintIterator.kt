// TARGET_BACKEND: JVM
// WITH_STDLIB

fun myFun(iterator: MyUIntIterator) {
    for (x in iterator) {
        assert(x == 42u)
    }
}

class MyUIntIterator : Iterator<UInt> {
    private var count = 1
    override fun hasNext(): Boolean = count-- > 0
    override fun next(): UInt = 42u
}

fun box(): String {
    myFun(MyUIntIterator())
    return "OK"
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/util/Iterator.hasNext \(\)Z
// 0 java/util/Iterator.next \(\)Ljava/lang/Object;
// 0 MyUIntIterator.next \(\)Ljava/lang/Object;
// 0 MyUIntIterator.hasNext \(\)Z
// 1 public synthetic bridge next\(\)Ljava/lang/Object;
// 2 MyUIntIterator.next-pVg5ArA \(\)I
// 0 INVOKEVIRTUAL kotlin/UInt.unbox-impl \(\)I