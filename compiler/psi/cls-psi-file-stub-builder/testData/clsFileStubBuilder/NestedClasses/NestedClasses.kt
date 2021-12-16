package test

class NestedClasses<TOuter> {
    fun f() {
    }

    val c: Int = 0

    private class Nested<TN> {
        fun f(p1: TN) {
        }

        val c = 1

        public class NN<TNN> {
            fun f(p1: TNN) {
            }

            val c = 1
        }

        inner class NI<TNI : TN> {
            fun f(p1: TN, p2: TNI) {
            }
        }
    }

    public inner class Inner<TI : TOuter> {
        fun f(p1: TI) {
        }

        private inner class II<TII> {
            fun f(p1: TII, p2: II<NestedClasses<TOuter>>, p3: TOuter) {
            }
        }
    }
}
