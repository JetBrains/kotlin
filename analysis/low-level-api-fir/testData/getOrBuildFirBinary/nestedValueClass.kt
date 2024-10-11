// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: LibraryNestedValueClass
package pack

class LibraryClass {
    @JvmInline
    value class LibraryNestedValueClass(val value: LibraryClass)
}
