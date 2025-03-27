// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE
package foo

annotation class Anno(val i: Int)

const val CONSTANT = 1

enum class MyEnumClass {
    Entry {
        @Anno(CONSTANT) @<!UNRESOLVED_REFERENCE{LT}!>UnresolvedAnno<!><!SYNTAX!><!>
    }
}
