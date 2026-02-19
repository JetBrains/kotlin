// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: MyDependency

package one

class A
class B
class C
class D

data class MyDependency(val b: B, var d: D) {
    val a: A get() = A()
    val c: C get() = C()
}
