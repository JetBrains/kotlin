// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

fun testCollections() {
    testKotlinSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; Set<String>")!>java.util.HashSet(setOf(null))<!>)
    testKotlinSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; Set<String>")!>HashSet(setOf(null))<!>)
    testKotlinList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; List<String>")!>java.util.ArrayList(arrayListOf(null))<!>)
    testKotlinList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; List<String>")!>ArrayList(arrayListOf(null))<!>)
    testKotlinMap(<!ARGUMENT_TYPE_MISMATCH("HashMap<Nothing?, Nothing?>; Map<String, String>")!>java.util.HashMap(hashMapOf(Pair(null, null)))<!>)
    testKotlinMap(<!ARGUMENT_TYPE_MISMATCH("HashMap<Nothing?, Nothing?>; Map<String, String>")!>HashMap(hashMapOf(Pair(null, null)))<!>)

    testJavaSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; HashSet<String>")!>java.util.HashSet(setOf(null))<!>)
    testJavaSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; HashSet<String>")!>HashSet(setOf(null))<!>)
    testJavaList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; ArrayList<String>")!>java.util.ArrayList(arrayListOf(null))<!>)
    testJavaList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; ArrayList<String>")!>ArrayList(arrayListOf(null))<!>)
    testJavaMap(<!ARGUMENT_TYPE_MISMATCH("HashMap<Nothing?, Nothing?>; HashMap<String, String>")!>java.util.HashMap(hashMapOf(Pair(null, null)))<!>)
    testJavaMap(<!ARGUMENT_TYPE_MISMATCH("HashMap<Nothing?, Nothing?>; HashMap<String, String>")!>HashMap(hashMapOf(Pair(null, null)))<!>)

    testKotlinSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; Set<String>")!>java.util.HashSet(setOf(null))<!>)
    testKotlinSet(<!ARGUMENT_TYPE_MISMATCH("HashSet<Nothing?>; Set<String>")!>java.util.HashSet(hashSetOf(null))<!>)
    testKotlinList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; List<String>")!>java.util.ArrayList(listOf(null))<!>)
    testKotlinList(<!ARGUMENT_TYPE_MISMATCH("ArrayList<Nothing?>; List<String>")!>java.util.ArrayList(arrayListOf(null))<!>)
}

fun testKotlinSet(a: Set<String>) {}
fun testKotlinList(a: List<String>) {}
fun testKotlinMap(a: Map<String, String>) {}

fun testJavaSet(a: java.util.HashSet<String>){}
fun testJavaList(a: java.util.ArrayList<String>) {}
fun testJavaMap(a: java.util.HashMap<String, String>) {}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType */
