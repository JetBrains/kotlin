// KT-587 Unresolved reference

class Main {
    class object {
        class States() {
            class object {
                public val N: States = States() // : States unresolved
            }
        }
    }
}
