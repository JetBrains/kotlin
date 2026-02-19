// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: A
// Issue with loading Compose libraries which were compiled with <=2.0 compiler (KTIJ-33020)
// FILE: Composable.kt
package androidx.compose.runtime

@Target(AnnotationTarget.TYPE)
annotation class Composable

// FILE: Usage.kt
import androidx.compose.runtime.Composable

class Inv<T>

class A {
    fun foo(action: @Composable (() -> Unit)) {}
    fun bar(action: Inv<@Composable (() -> Unit)>): @Composable () -> Unit = null!!
}
