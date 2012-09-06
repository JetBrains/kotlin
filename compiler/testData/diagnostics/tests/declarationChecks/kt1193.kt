//KT-1193 Check enum entry supertype

package kt1193

enum class MyEnum(val i: Int) {
    A : MyEnum(12)
    <!ENUM_ENTRY_SHOULD_BE_INITIALIZED!>B<!>  //no error
}