// WITH_STDLIB

@JvmInline
value class IntHolder(val value: Int)

@JvmInline
value class StringHolder(val value: String)

@OptIn(ExperimentalUnsignedTypes::class)
fun test(): Int {
    val u: UInt = 1u
    val bytes = ubyteArrayOf(253u, 254u, 255u)
    val ih = IntHolder(2)
    val sh = StringHolder("a")
    val nullableSh: StringHolder? = StringHolder("b")
    val boxed: UInt? = 3u
    val (first, second) = Pair(u, ih)
    var sum = 0
    for (element in bytes) {
        sum += element.toInt()
    }
    return sum + first.toInt() + second.value + sh.value.length + (nullableSh?.value?.length ?: 0) + (boxed?.toInt() ?: 0)
}

// METHOD : InlineClassLocalsKt.test()I
// VARIABLE : NAME=$v$c$IntHolder$$ih TYPE=I
// VARIABLE : NAME=$v$c$IntHolder$$second TYPE=I
// VARIABLE : NAME=$v$c$StringHolder$$nullableSh TYPE=Ljava/lang/String;
// VARIABLE : NAME=$v$c$StringHolder$$sh TYPE=Ljava/lang/String;
// VARIABLE : NAME=$v$c$kotlin$_UByte$$element TYPE=B
// VARIABLE : NAME=$v$c$kotlin$_UByteArray$$bytes TYPE=[B
// VARIABLE : NAME=$v$c$kotlin$_UInt$$first TYPE=I
// VARIABLE : NAME=$v$c$kotlin$_UInt$$u TYPE=I
// The nullable `boxed` is boxed, so its name is not encoded. Then the redundant boxing optimization removes the box.
// VARIABLE : NAME=boxed TYPE=I
// VARIABLE : NAME=sum TYPE=I
