// FILE: TypeAnno.kt
@Target(AnnotationTarget.TYPE)
annotation class TypeAnno

// FILE: RegularAnno.kt
annotation class RegularAnno

// FILE: MyInterface.kt
interface MyInterface {
    @RegularAnno
    val property: @TypeAnno String

    @RegularAnno
    fun function(@RegularAnno argument: @TypeAnno Int): @TypeAnno Int
}
