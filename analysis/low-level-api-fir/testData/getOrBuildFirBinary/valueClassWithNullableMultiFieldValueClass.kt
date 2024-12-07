// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: MyValueClass
// LANGUAGE: +ValueClasses
package pack

@JvmInline
value class MyValueClass(val foo: Foo<Int>?)

@JvmInline
value class Foo<T>(val a: T, val b: T)