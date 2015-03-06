abstract class ClassValAbstract {
    abstract var a: Int

    default object {
        val methods = (this as java.lang.Object).getClass()?.getClassLoader()?.loadClass("ClassValAbstract")?.getMethods()!!
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
