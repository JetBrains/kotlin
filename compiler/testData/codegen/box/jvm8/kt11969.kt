// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Z {
    private fun privateFun() = { "OK" }

    fun callPrivateFun() = privateFun()

    fun publicFun() = { "OK" }

    fun funWithDefaultArgs(s: () -> Unit = {}): () -> Unit

    val property: () -> Unit
        get() = {}

    class Nested
}

class Test : Z {
    override fun funWithDefaultArgs(s: () -> Unit): () -> Unit {
        return s
    }
}

fun box(): String {

    val privateFun = Test().callPrivateFun()
    var enclosing = privateFun.javaClass.enclosingMethod!!
    if (enclosing.name != "privateFun") return "fail 1: ${enclosing.name}"
    if (enclosing.getDeclaringClass().simpleName != "DefaultImpls") return "fail 2: ${enclosing.getDeclaringClass().simpleName}"

    val publicFun = Test().publicFun()
    enclosing = publicFun.javaClass.enclosingMethod!!
    if (enclosing.name != "publicFun") return "fail 3: ${enclosing.name}"
    if (enclosing.getDeclaringClass().simpleName != "DefaultImpls") return "fail 4: ${enclosing.getDeclaringClass().simpleName}"

    val property = Test().property
    enclosing = property.javaClass.enclosingMethod!!
    if (enclosing.name != "getProperty") return "fail 4: ${enclosing.name}"
    if (enclosing.getDeclaringClass().simpleName != "DefaultImpls") return "fail 5: ${enclosing.getDeclaringClass().simpleName}"

    val defaultArgs = Test().funWithDefaultArgs()
    enclosing = defaultArgs.javaClass.enclosingMethod!!
    if (enclosing.name != "funWithDefaultArgs\$default") return "fail 6: ${enclosing.name}"
    if (enclosing.parameterTypes.size != 4) return "fail 7: not default method ${enclosing.name}"
    if (enclosing.getDeclaringClass().simpleName != "DefaultImpls") return "fail 8: ${enclosing.getDeclaringClass().simpleName}"

    val nested = Z.Nested::class.java
    val enclosingClass = nested.enclosingClass!!
    if (enclosingClass.name != "Z") return "fail 9: ${enclosingClass.name}"

    return "OK"
}
