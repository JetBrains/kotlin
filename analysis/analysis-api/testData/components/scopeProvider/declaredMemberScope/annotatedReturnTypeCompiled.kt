// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
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

// MODULE: main(lib)
// FILE: main.kt
// class: test/MyInterface