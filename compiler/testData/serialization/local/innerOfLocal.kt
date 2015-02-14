// CLASS_NAME_SUFFIX: main$Local$Inner

fun main() {
    class Local {
        inner class Inner {
            fun local(l: Local) {}
            fun inner(i: Inner) {}
        }
    }
}
