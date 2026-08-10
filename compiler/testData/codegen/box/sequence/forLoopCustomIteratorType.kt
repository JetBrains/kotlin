// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

class MyIter(private val n: Int) : Iterator<Int> {
    private var i = 0
    override fun hasNext() = i < n
    override fun next() = i++
}

class MySeq(private val n: Int) : Sequence<Int> {
    override fun iterator(): MyIter = MyIter(n)
}

interface MySeqInterface : Sequence<Int> {
    override fun iterator(): MyIter
}

class MySeqImpl(private val n: Int) : MySeqInterface {
    override fun iterator(): MyIter = MyIter(n)
}

fun sumClass(s: MySeq): Int {
    var sum = 0
    for (x in s) sum += x
    return sum
}

fun sumInterface(s: MySeqInterface): Int {
    var sum = 0
    for (x in s) sum += x
    return sum
}

fun box(): String {
    val classSum = sumClass(MySeq(4))
    if (classSum != 6) return "Fail class: $classSum"
    val interfaceSum = sumInterface(MySeqImpl(4))
    if (interfaceSum != 6) return "Fail interface: $interfaceSum"
    return "OK"
}
