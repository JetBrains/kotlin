// MEMBER_NAME_FILTER: resolveMe
package second

abstract class S<caret>ubClass: AbstractClass<String>()

abstract class AbstractClass<T> {
    fun explicitType(): T? = null
    fun T.resolveMe() = explicitType()
}
