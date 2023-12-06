package test

val property: Int = 10

enum class MyEnum(val i: Int) {
    VARIANT(property) {
        fun test() {
            proper<caret>ty
        }
    }
}