// KT-587 Unresolved reference

class Main {
    default object {
        class States() {
            default object {
                public val N: States = States() // : States unresolved
            }
        }
    }
}
