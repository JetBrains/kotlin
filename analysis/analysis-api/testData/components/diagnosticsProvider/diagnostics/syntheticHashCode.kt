// ISSUE: KT-74518
// IGNORE_SELF_MODE
// IGNORE_DANGLING_FILES
package one

class TopLevel {
    data class State(val i: Int) : InterfaceWithParameter<State>
}

interface InterfaceWithParameter<T>
