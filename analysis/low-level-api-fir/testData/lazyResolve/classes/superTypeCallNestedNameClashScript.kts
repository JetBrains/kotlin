package second

open class Base<T>

class MyCla<caret>ss() : Base<Base<Int>>() {
    open class Base<T>
}
