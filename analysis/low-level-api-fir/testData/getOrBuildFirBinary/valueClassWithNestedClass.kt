// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: LibraryValueClass
package pack

@JvmInline
value class LibraryValueClass(val value: NestedClass) {
    class NestedClass
}
