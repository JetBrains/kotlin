// ISSUE: KT-82948
// MEMBER_NAME_FILTER: hashCode
// IGNORE_FIR
package one

class TopLevel {
    typealias State = String
    data class St<caret>ate(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>
