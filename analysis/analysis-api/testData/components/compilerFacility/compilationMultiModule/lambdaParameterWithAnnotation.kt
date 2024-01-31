// MODULE: compose
// FILE: p2/bar.kt
package p2;

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Composable
// MODULE: lib(compose)
// FILE: p3/foo.kt
package p3;

import p2.Composable

fun setContent(content: @Composable () -> Unit): Int {
    content()
    return 3
}
// MODULE: main(lib, compose)
// FILE: main.kt
import p2.Composable
import p3.setContent

fun test(): Int {
    return setContent {
        Greeting("test")
    }
}

@Composable
fun Greeting(name: String) {
    show("hi $name!")
}

fun show(str: String) {}