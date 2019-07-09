// WITH_RUNTIME
// DISABLE-ERRORS
package foo.www.ddd

class Check {
    class BBD {
        class Bwd {
            fun dad() {
                class Bwd
                val a = foo.www.ddd.<caret>Check.BBD.Bwd::class.java.annotatedInterfaces.size
            }
        }
    }
}
