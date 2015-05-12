// "Change type from 'A' to 'B'" "true"
interface A {}
interface B : A {}

fun foo(f: (B) -> B) {
    foo {
        (b: B): A<caret> -> b
    }
}