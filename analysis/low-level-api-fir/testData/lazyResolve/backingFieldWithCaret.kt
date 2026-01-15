// MEMBER_NAME_FILTER: prop
// LANGUAGE: +ExplicitBackingFields
package m2

abstract class My<caret>Class<T>(x: MutableList<T>) {
    val prop: Any?
        field = x
}
