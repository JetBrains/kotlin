// ISSUE: KT-58940

// FILE: JavaIndex.java
public class JavaIndex {
    public String getIndexer() { return ""; }
}

// FILE: main.kt
abstract class KotlinIndex : JavaIndex() {
    fun indexer(x: Int): String = ""
}

class MyKotlinIndex : KotlinIndex() {
    // `indexer(1)` call should just request the member function that might be resolved successfully
    // and should not request `indexer` variable (as invoke candidates are anyway less prioritized), thus avoiding TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM
    val INDEXER = indexer(1)

    override fun getIndexer() = INDEXER
}

fun main() {
    MyKotlinIndex().getIndexer().length
}