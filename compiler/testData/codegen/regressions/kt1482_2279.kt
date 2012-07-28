abstract class ClassValAbstract {
    abstract var a: Int

    class object {
        val methods = (this as java.lang.Object).getClass()?.getClassLoader()?.loadClass("ClassValAbstract")?.getMethods()
    }
}

fun box() : String {
    for(m in ClassValAbstract.methods) {
        if (m.sure().getName() == "getA") {
            if(m.sure().getModifiers() != 1025)
                return "get failed"
        }
        if (m.sure().getName() == "setA") {
            if(m.sure().getModifiers() != 1025)
                return "set failed"
        }
    }
    return "OK"
}
