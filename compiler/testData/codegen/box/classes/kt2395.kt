// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

import java.util.AbstractList

class MyList(): AbstractList<String>() {
     public fun getModificationCount(): Int = modCount
     public override fun get(index: Int): String = ""
     public override val size: Int get() = 0
}

fun box(): String {
    return if (MyList().getModificationCount() == 0) "OK" else "fail"
}
