// ISSUE: KT-74518
// MEMBER_NAME_FILTER: hashCode
package one

class TopLevel {
    data class Sta<caret>te(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>
