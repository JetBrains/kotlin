// MEMBER_NAME_FILTER: resolveMe
package second

abstract class S<caret>ubClass: AbstractClass<Int>()

abstract class AbstractClass<T> {
    abstract fun T.resolveMe(param: T): T
}
