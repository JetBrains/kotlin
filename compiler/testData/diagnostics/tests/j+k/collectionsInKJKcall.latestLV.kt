// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

fun testCollections() {
    testKotlinSet(java.util.HashSet(setOf(null)))
    testKotlinSet(HashSet(setOf(null)))
    testKotlinList(java.util.ArrayList(arrayListOf(null)))
    testKotlinList(ArrayList(arrayListOf(null)))
    testKotlinMap(java.util.HashMap(hashMapOf(Pair(null, null))))
    testKotlinMap(HashMap(hashMapOf(Pair(null, null))))

    testJavaSet(java.util.HashSet(setOf(null)))
    testJavaSet(HashSet(setOf(null)))
    testJavaList(java.util.ArrayList(arrayListOf(null)))
    testJavaList(ArrayList(arrayListOf(null)))
    testJavaMap(java.util.HashMap(hashMapOf(Pair(null, null))))
    testJavaMap(HashMap(hashMapOf(Pair(null, null))))

    testKotlinSet(java.util.HashSet(setOf(null)))
    testKotlinSet(java.util.HashSet(hashSetOf(null)))
    testKotlinList(java.util.ArrayList(listOf(null)))
    testKotlinList(java.util.ArrayList(arrayListOf(null)))
}

fun testKotlinSet(a: Set<String>) {}
fun testKotlinList(a: List<String>) {}
fun testKotlinMap(a: Map<String, String>) {}

fun testJavaSet(a: java.util.HashSet<String>){}
fun testJavaList(a: java.util.ArrayList<String>) {}
fun testJavaMap(a: java.util.HashMap<String, String>) {}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType */
