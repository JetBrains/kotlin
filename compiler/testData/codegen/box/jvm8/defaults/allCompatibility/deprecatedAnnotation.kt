// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_STDLIB

interface IFoo {
    fun foo() {}
}

fun box(): String {
    val iFoo = IFoo::class.java
    val iFooDefaultImpls = Class.forName("${iFoo.name}\$DefaultImpls")
    val fooMethod = iFooDefaultImpls.declaredMethods.find { it.name == "foo" }
        ?: throw AssertionError("No method 'foo' in class ${iFooDefaultImpls.name}")
    fooMethod.getAnnotation(java.lang.Deprecated::class.java)
        ?: throw AssertionError("No java.lang.Deprecated annotation on method 'foo'")
    return "OK"
}
