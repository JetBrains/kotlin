// FULL_JDK
// WITH_STDLIB

fun testCollections() {
    testKotlinSet(<!TYPE_MISMATCH!>java.util.HashSet(setOf(null))<!>)
    testKotlinSet(<!TYPE_MISMATCH!>HashSet(setOf(null))<!>)
    testKotlinList(java.util.ArrayList(arrayListOf(null)))
    testKotlinList(<!TYPE_MISMATCH!>ArrayList(arrayListOf(null))<!>)
    testKotlinMap(java.util.HashMap(hashMapOf(Pair(null, null))))
    testKotlinMap(<!TYPE_MISMATCH!>HashMap(hashMapOf(Pair(null, null)))<!>)

    testJavaSet(java.util.HashSet(setOf(null)))
    testJavaSet(<!TYPE_MISMATCH!>HashSet(setOf(null))<!>)
    testJavaList(java.util.ArrayList(arrayListOf(null)))
    testJavaList(<!TYPE_MISMATCH!>ArrayList(arrayListOf(null))<!>)
    testJavaMap(java.util.HashMap(hashMapOf(Pair(null, null))))
    testJavaMap(<!TYPE_MISMATCH!>HashMap(hashMapOf(Pair(null, null)))<!>)

    testKotlinSet(<!TYPE_MISMATCH!>java.util.HashSet(setOf(null))<!>)
    testKotlinSet(java.util.HashSet(hashSetOf(null)))
    testKotlinList(<!TYPE_MISMATCH!>java.util.ArrayList(listOf(null))<!>)
    testKotlinList(java.util.ArrayList(arrayListOf(null)))
}

fun testKotlinSet(a: Set<String>) {}
fun testKotlinList(a: List<String>) {}
fun testKotlinMap(a: Map<String, String>) {}

fun testJavaSet(a: java.util.HashSet<String>){}
fun testJavaList(a: java.util.ArrayList<String>) {}
fun testJavaMap(a: java.util.HashMap<String, String>) {}