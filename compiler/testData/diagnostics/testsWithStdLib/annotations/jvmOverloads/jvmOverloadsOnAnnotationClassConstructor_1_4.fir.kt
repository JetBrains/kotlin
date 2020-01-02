// !LANGUAGE: +ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses

annotation class A1 @JvmOverloads constructor(val x: Int = 1)
annotation class A2 @JvmOverloads constructor()