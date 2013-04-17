// "Change type from 'A' to 'B'" "true"
trait A {}
trait B : A {}

fun foo(f: (B) -> B) {
    foo {
        (b: B): A<caret> -> b
    }
}