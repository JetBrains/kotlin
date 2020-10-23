// "Add '@OptIn(A::class)' annotation to 'root'" "true"
// WITH_RUNTIME
@RequiresOptIn
annotation class A

@A
fun f1() {}

@OptIn
fun root() {
    <caret>f1()
}