// NEXT_SIBLING:
class A {
    class B {
        fun test(): Int {
            <selection>coFun()
            return coProp + 10</selection>
        }

        class object {
            val coProp = 1

            fun coFun() {

            }
        }
    }
}


