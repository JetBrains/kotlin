// "Make 'Companion' public explicitly" "true"
// COMPILER_ARGUMENTS: -Xexplicit-api=strict

public class Foo1() {
    companion <caret>object {}
}

