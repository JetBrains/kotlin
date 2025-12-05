// ISSUE: KT-82948
// IGNORE_FIR
package one

class TopLevel {
    typealias State = String
    data class State(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>
