//KT-1193 Check enum entry supertype

package kt1193

open enum class MyEnum(val i: Int) {
    A : MyEnum(12)
    <!ENUM_ENTRY_SHOULD_BE_INITIALIZED!>B<!>  //no error
}

enum class MyChildEnum(i: Int, val s: String) : MyEnum(i) {
    C : <!ENUM_ENTRY_ILLEGAL_TYPE!>MyEnum<!>(3)
}
