// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// ISSUE: KT-81722

@file:OptIn(ExperimentalCollectionLiterals::class, ExperimentalUnsignedTypes::class)

fun box(): String {
    val list0: () -> List<String> = List.Companion::of
    val list1: (String) -> List<String> = List.Companion::of
    val listM: (Array<String>) -> List<String> = List.Companion::of

    if (list0() != emptyList<String>()) return "Fail#List0"
    if (list1("single") != listOf("single")) return "Fail#List1"
    if (listM(arrayOf("1", "2", "3")) != listOf("1", "2", "3")) return "Fail#ListM"

    val mutableList0: () -> MutableList<String> = MutableList.Companion::of
    val mutableList1: (String) -> MutableList<String> = MutableList.Companion::of
    val mutableListM: (Array<String>) -> MutableList<String> = MutableList.Companion::of

    if (mutableList0() != mutableListOf<String>()) return "Fail#MutableList0"
    if (mutableList1("single") != mutableListOf("single")) return "Fail#MutableList1"
    if (mutableListM(arrayOf("1", "2", "3")) != mutableListOf("1", "2", "3")) return "Fail#MutableListM"

    val set0: () -> Set<String> = Set.Companion::of
    val set1: (String) -> Set<String> = Set.Companion::of
    val setM: (Array<String>) -> Set<String> = Set.Companion::of

    if (set0() != emptySet<String>()) return "Fail#Set0"
    if (set1("single") != setOf("single")) return "Fail#Set1"
    if (setM(arrayOf("1", "2", "3")) != setOf("1", "2", "3")) return "Fail#SetM"

    val mutableSet0: () -> MutableSet<String> = MutableSet.Companion::of
    val mutableSet1: (String) -> MutableSet<String> = MutableSet.Companion::of
    val mutableSetM: (Array<String>) -> MutableSet<String> = MutableSet.Companion::of

    if (mutableSet0() != mutableSetOf<String>()) return "Fail#MutableSet0"
    if (mutableSet1("single") != mutableSetOf("single")) return "Fail#MutableSet1"
    if (mutableSetM(arrayOf("1", "2", "3")) != mutableSetOf("1", "2", "3")) return "Fail#MutableSetM"

    val sequence0: () -> Sequence<String> = Sequence.Companion::of
    val sequence1: (String) -> Sequence<String> = Sequence.Companion::of
    val sequenceM: (Array<String>) -> Sequence<String> = Sequence.Companion::of

    if (sequence0().toList() != emptyList<String>()) return "Fail#Sequence0"
    if (sequence1("single").toList() != listOf("single")) return "Fail#Sequence1"
    if (sequenceM(arrayOf("1", "2", "3")).toList() != listOf("1", "2", "3")) return "Fail#SequenceM"

    val array: (Array<String>) -> Array<String> = Array.Companion::of
    if (!array(arrayOf("single")).contentEquals(arrayOf("single"))) return "Fail#Array"

    val intArray: (IntArray) -> IntArray = IntArray::of
    if (!intArray(intArrayOf(42)).contentEquals(intArrayOf(42))) return "Fail#IntArray"

    val byteArray: (ByteArray) -> ByteArray = ByteArray::of
    if (!byteArray(byteArrayOf(42)).contentEquals(byteArrayOf(42))) return "Fail#ByteArray"

    val shortArray: (ShortArray) -> ShortArray = ShortArray::of
    if (!shortArray(shortArrayOf(42)).contentEquals(shortArrayOf(42))) return "Fail#ShortArray"

    val longArray: (LongArray) -> LongArray = LongArray::of
    if (!longArray(longArrayOf(42L)).contentEquals(longArrayOf(42L))) return "Fail#LongArray"

    val charArray: (CharArray) -> CharArray = CharArray::of
    if (!charArray(charArrayOf('a')).contentEquals(charArrayOf('a'))) return "Fail#CharArray"

    val booleanArray: (BooleanArray) -> BooleanArray = BooleanArray::of
    if (!booleanArray(booleanArrayOf(true)).contentEquals(booleanArrayOf(true))) return "Fail#BooleanArray"

    val floatArray: (FloatArray) -> FloatArray = FloatArray::of
    if (!floatArray(floatArrayOf(42.0f)).contentEquals(floatArrayOf(42.0f))) return "Fail#FloatArray"

    val doubleArray: (DoubleArray) -> DoubleArray = DoubleArray::of
    if (!doubleArray(doubleArrayOf(42.0)).contentEquals(doubleArrayOf(42.0))) return "Fail#DoubleArray"

    val ubyteArray: (UByteArray) -> UByteArray = UByteArray::of
    if (!ubyteArray(ubyteArrayOf(42u)).contentEquals(ubyteArrayOf(42u))) return "Fail#UByteArray"

    val ushortArray: (UShortArray) -> UShortArray = UShortArray::of
    if (!ushortArray(ushortArrayOf(42u)).contentEquals(ushortArrayOf(42u))) return "Fail#UShortArray"

    val uintArray: (UIntArray) -> UIntArray = UIntArray::of
    if (!uintArray(uintArrayOf(42u)).contentEquals(uintArrayOf(42u))) return "Fail#UIntArray"

    val ulongArray: (ULongArray) -> ULongArray = ULongArray::of
    if (!ulongArray(ulongArrayOf(42UL)).contentEquals(ulongArrayOf(42UL))) return "Fail#ULongArray"
    
    return "OK"
}
