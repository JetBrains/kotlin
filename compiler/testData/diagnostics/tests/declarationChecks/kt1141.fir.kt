//KT-1141 No check that object in 'object expression' implements all abstract members of supertype

package kt1141

public interface SomeTrait {
    fun foo()
}

fun foo() {
    val x = object : SomeTrait {
    }
    x.foo()
}

object Rr : SomeTrait {}

class C : SomeTrait {}

fun foo2() {
    val r = object : Runnable {} //no error
}
