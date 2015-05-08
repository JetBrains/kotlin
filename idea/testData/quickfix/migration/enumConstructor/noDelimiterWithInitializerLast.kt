// "Change to short enum entry super constructor" "true"

enum class MyEnum(val z: Int) {
    A(3)
    B(7)
    C: MyEnum(12)<caret>
    fun foo() = z * 2
}