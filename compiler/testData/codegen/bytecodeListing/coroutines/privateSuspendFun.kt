// IGNORE_BACKEND: JVM_IR
// TODO: KT-37086
private suspend fun foo() {}

class A {
    private suspend fun foo() {}
}
