// RUN_PIPELINE_TILL: FRONTEND
package foo

annotation class Anno(val i: Int)

const val CONSTANT = 1

enum class MyEnumClass {
    Entry {
        @Anno(CONSTANT) @<!UNRESOLVED_REFERENCE!>UnresolvedAnno<!><!SYNTAX!><!>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, const, enumDeclaration, enumEntry, integerLiteral, primaryConstructor,
propertyDeclaration */
