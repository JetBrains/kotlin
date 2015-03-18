// KT-587 Unresolved reference

class Main {
    companion object {
        class States() {
            companion object {
                public val N: States = States() // : States unresolved
            }
        }
    }
}
