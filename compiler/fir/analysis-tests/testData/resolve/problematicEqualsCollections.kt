// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNSUPPORTED

fun test1(a: List<Int>, b: Set<Int>): Boolean = <!PROBLEMATIC_EQUALS!>a == b<!>
fun test2(a: Set<Int>, b: List<Int>): Boolean = <!PROBLEMATIC_EQUALS!>a == b<!>
fun test3(a: List<Int>, b: MutableList<Int>): Boolean = a == b
fun test4(a: Set<Int>, b: MutableList<Int>): Boolean = <!PROBLEMATIC_EQUALS!>a == b<!>
fun test5(a: List<Int>, b: ArrayList<Int>): Boolean = a == b
fun test6(a: HashSet<Int>, b: Set<Int>): Boolean = a == b
fun test7(a: HashSet<Int>, b: LinkedHashSet<Int>): Boolean = a == b
fun test8(a: List<Int>, b: dynamic): Boolean = a == b

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration */
