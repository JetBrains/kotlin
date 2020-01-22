// "Make 'method' public explicitly" "true"
// COMPILER_ARGUMENTS: -Xexplicit-api=strict

public class Foo2() {
    fun <caret>method() {}
}
