abstract class ClassValAbstract {
    abstract val a: Int

    class object {
        val methods = (this as java.lang.Object).getClass()?.classLoader?.loadClass("ClassValAbstract")?.getMethods()
    }
}

fun box() : String {
    for(m in ClassValAbstract.methods) {
        if(m.sure().getName() == "getA") {
            if(m.sure().getModifiers() != 1024)
                return "get failed"
        }
        if(m.sure().getName() == "setA") {
            if(m.sure().getModifiers() != 1024)
                return "set failed"
        }
    }
    return "OK"
}