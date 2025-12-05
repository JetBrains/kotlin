// ISSUE: KT-82948
package one

class TopLevel {
    typealias State = String
    data class State(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>

// SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK: KT-63221
