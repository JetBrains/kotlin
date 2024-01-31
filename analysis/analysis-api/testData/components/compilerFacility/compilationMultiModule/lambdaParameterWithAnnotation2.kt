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

@Composable
public fun Foo(
    text: @Composable () -> Unit,
) {}
// MODULE: main(lib, compose)
// FILE: main.kt
import p2.Composable
import p3.Foo

@Composable
public fun Bar() {
    Foo(
        text = {}, // @Composable invocations can only happen from the context of a @Composable function
    )
}