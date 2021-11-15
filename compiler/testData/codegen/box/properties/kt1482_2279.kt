// TARGET_BACKEND: JVM
// WITH_STDLIB
package test

abstract class ClassValAbstract {
    abstract var a: Int

    companion object {
        val methods = ClassValAbstract::class.java.getMethods()!!
    }
}

fun box() : String {
    for(m in ClassValAbstract.methods) {
        if (m!!.getName() == "getA") {
            if(m!!.getModifiers() != 1025)
                return "get failed"
        }
        if (m!!.getName() == "setA") {
            if(m!!.getModifiers() != 1025)
                return "set failed"
        }
    }
    return "OK"
}
