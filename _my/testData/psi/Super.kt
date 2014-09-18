// KT-156 Fix the this<Super> syntax
fun foo() {
    super.foo();
    super<Int>.foo();
    super<>.foo();
    super<Int>@label.foo();
}