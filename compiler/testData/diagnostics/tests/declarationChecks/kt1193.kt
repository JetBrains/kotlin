// !DIAGNOSTICS: -UNUSED_PARAMETER
//KT-1193 Check enum entry supertype / initialization

package kt1193

enum class MyEnum(val i: Int) {
    A(12),
    <!ENUM_ENTRY_SHOULD_BE_INITIALIZED!>B<!>  //no error
}

open class A(x: Int = 1)

val x: MyEnum = MyEnum.A