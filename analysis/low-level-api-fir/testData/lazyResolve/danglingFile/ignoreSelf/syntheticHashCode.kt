// ISSUE: KT-74518
// MEMBER_NAME_FILTER: hashCode
// IGNORE_FIR
package one

class TopLevel {
    data class Sta<caret>te(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>
