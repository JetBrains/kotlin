// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: MyInterface

package test

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno
annotation class RegularAnno

interface MyInterface {
    @RegularAnno
    val property: @TypeAnno String

    @RegularAnno
    fun function(@RegularAnno argument: @TypeAnno Int): @TypeAnno Int
}
