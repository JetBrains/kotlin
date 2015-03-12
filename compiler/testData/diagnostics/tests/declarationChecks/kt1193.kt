// !DIAGNOSTICS: -UNUSED_PARAMETER
//KT-1193 Check enum entry supertype

package kt1193

enum class MyEnum(val i: Int) {
    A : MyEnum(12)
    <!ENUM_ENTRY_SHOULD_BE_INITIALIZED!>B<!>  //no error
}

open class A(x: Int = 1)
enum class MyOtherEnum(val i: Int) {
    X : <!ENUM_ENTRY_ILLEGAL_TYPE!>A<!>(3)
    Y : <!ENUM_ENTRY_ILLEGAL_TYPE!>A<!>()
    Z : <!ENUM_ENTRY_ILLEGAL_TYPE!>A<!>(3) {}
}
