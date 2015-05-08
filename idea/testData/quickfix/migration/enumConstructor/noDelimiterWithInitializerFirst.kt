// "Change to short enum entry super constructor" "true"

enum class MyEnum(val z: Int) {
    A: MyEnum(3)<caret>
    B(7)
    C(12)
    fun foo() = z * 2
}