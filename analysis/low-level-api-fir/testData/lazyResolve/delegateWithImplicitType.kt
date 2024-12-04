// MEMBER_NAME_FILTER: implicitType

class Aa<caret>a(i: Interface) : Interface by i

interface Interface {
    fun implicitType() = 42
}
