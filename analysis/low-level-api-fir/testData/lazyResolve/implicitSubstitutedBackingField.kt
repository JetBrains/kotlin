// MEMBER_NAME_FILTER: prop
// LANGUAGE: +ExplicitBackingFields
package m2

abstract class MyClass<T>(x: MutableList<T>) {
    val prop: Any?
        field = x
}

class MyI<caret>mpl : MyClass<Int>(null!!)
