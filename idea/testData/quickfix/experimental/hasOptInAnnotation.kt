// "Add '@OptIn(B::class)' annotation to 'root'" "true"
// WITH_RUNTIME
@RequiresOptIn
annotation class A

@RequiresOptIn
annotation class B

@A
fun f1() {}

@B
fun f2() {}

@OptIn(A::class)
fun root() {
    f1()
    <caret>f2()
}