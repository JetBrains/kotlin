// SHOULD_FAIL_WITH: Usages of outer class instance inside of property 'y' won't be processed
class A {
    val x = 1
    val <caret>y = x + 1
}