// "Change to short enum entry super constructor in the whole project" "true"

enum class MyEnum(val i: Int) {
    // The 
    // first
    FIRST: MyEnum(1), 
    // The 
    // second
    SECOND: MyEnum(2),
    // The 
    // third
    THIRD: MyEnum(3)<caret>
}