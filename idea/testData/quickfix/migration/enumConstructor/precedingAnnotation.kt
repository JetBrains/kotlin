// "Change to short enum entry super constructor in the whole project" "true"

annotation class My
annotation class Your
annotation class His

enum class MyEnum(val i: Int) {
    @My FIRST: MyEnum(1)<caret>, 
    @My @Your SECOND: MyEnum(2),
    @Your @His THIRD: MyEnum(3)
}