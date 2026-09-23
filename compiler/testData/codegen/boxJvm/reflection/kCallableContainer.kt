// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_REFLECT

// FILE: J.java
class J {
    public int field;
    public static int sField;
    public void foo() {}
    public static void sFoo() {}
}

// FILE: test.kt

import kotlin.test.*
import kotlin.reflect.*

class DelegateExposeContainer {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): KDeclarationContainer? = property.container
}

class C {
    fun memberFun() {}
    fun String.memberExtFun() {}
    val memberVal = ""
    val String.memberExtVal get() = ""
    var memberVar = 0
    val delegated by DelegateExposeContainer()

    fun testDelegatedLocalVal() {
        val prop by DelegateExposeContainer()
        assertEquals(null, prop)
    }

    companion {
        fun blockFun() {}
        val blockVal = ""
        var blockVar = 0
        val blockDelegated by DelegateExposeContainer()
    }

    companion object {
        fun companionObjectFun() {}
        fun String.companionObjectExtFun() {}
        val companionObjectVal = ""
        val String.companionObjectExtVal get() = ""
        var companionObjectVar = 0
        val companionObjectDelegated by DelegateExposeContainer()
    }
}

interface I {
    fun memberFun() {}
    fun String.memberExtFun() {}
    val memberVal: String
    val String.memberExtVal: String
    var memberVar: String

    companion {
        fun blockFun() {}
// TODO: uncomment when KT-85853 is fixed
//        val blockVal = ""
//        var blockVar = 0
//        val blockDelegated by DelegateExposeContainer()
    }

    companion object {
        fun companionObjectFun() {}
        fun String.companionObjectExtFun() {}
        val companionObjectVal = ""
        val String.companionObjectExtVal get() = ""
        var companionObjectVar = 0
        val companionObjectDelegated by DelegateExposeContainer()
    }
}

fun toplevelFun() {}
fun String.toplevelExtFun() {}
val toplevelVal = ""
val String.toplevelExtVal get() = ""
var toplevelVar = 0
val toplevelDelegated by DelegateExposeContainer()

class E

companion fun E.compExtFun() {}
companion val E.compExtVal get() = ""
companion var E.compExtVar get() = ""; set(value) {}
companion val E.compExtDelegated by DelegateExposeContainer()

fun box(): String {
    J::class.members.forEach { assertEquals(J::class, it.container) }
    C::class.members.forEach { assertEquals(C::class, it.container) }
    I::class.members.forEach { assertEquals(I::class, it.container) }

    assertEquals(J::class, J::field.container)
    assertEquals(J::class, J::sField.container)
    assertEquals(J::class, J::foo.container)
    assertEquals(J::class, J::sFoo.container)

    assertEquals(C::class, C::memberFun.container)
    assertEquals(C::class, C::memberVal.container)
    assertEquals(C::class, C::memberVar.container)
    assertEquals(C::class, C::delegated.container)
    assertEquals(C::class, C().delegated)
    assertEquals(C::class, C::blockFun.container)
    assertEquals(C::class, C::blockVal.container)
    assertEquals(C::class, C::blockVar.container)
    assertEquals(C::class, C::blockDelegated.container)
    assertEquals(C::class, C.blockDelegated)
    assertEquals(C.Companion::class, C::companionObjectFun.container)
    assertEquals(C.Companion::class, C::companionObjectVal.container)
    assertEquals(C.Companion::class, C::companionObjectVar.container)
    assertEquals(C.Companion::class, C::companionObjectDelegated.container)
    assertEquals(C.Companion::class, C.companionObjectDelegated)

    assertEquals(I::class, I::memberFun.container)
    assertEquals(I::class, I::memberVal.container)
    assertEquals(I::class, I::memberVar.container)
    assertEquals(I::class, I::blockFun.container)
// TODO: uncomment when KT-85853 is fixed
//    assertEquals(I::class, I::blockVal.container)
//    assertEquals(I::class, I::blockVar.container)
//    assertEquals(I::class, I::blockDelegated.container)
//    assertEquals(I::class, I.blockDelegated)
    assertEquals(I.Companion::class, I::companionObjectFun.container)
    assertEquals(I.Companion::class, I::companionObjectVal.container)
    assertEquals(I.Companion::class, I::companionObjectVar.container)
    assertEquals(I.Companion::class, I::companionObjectDelegated.container)
    assertEquals(I.Companion::class, I.companionObjectDelegated)

    val fileClass = ::box.container
    assertEquals("file class TestKt", fileClass.toString())

    assertEquals(fileClass, ::toplevelFun.container)
    assertEquals(fileClass, String::toplevelExtFun.container)
    assertEquals(fileClass, ::toplevelVal.container)
    assertEquals(fileClass, String::toplevelExtVal.container)
    assertEquals(fileClass, ::toplevelVar.container)
    assertEquals(fileClass, ::toplevelDelegated.container)
    assertEquals(fileClass, toplevelDelegated)
    assertEquals(fileClass, E::compExtFun.container)
    assertEquals(fileClass, E::compExtVal.container)
    assertEquals(fileClass, E::compExtVar.container)
    assertEquals(fileClass, E::compExtDelegated.container)
    assertEquals(fileClass, E.compExtDelegated)

    fun local() {}
    assertEquals(null, ::local.container)

    val delegatedLocal by DelegateExposeContainer()
    assertEquals(null, delegatedLocal)
    C().testDelegatedLocalVal()

    val obj = object {
        fun memberFun() {}
        fun String.memberExtFun() {}
        val memberVal: String = ""
        val String.memberExtVal: String get() = ""
        var memberVar: String = ""
        val delegated by DelegateExposeContainer()
    }
    assertEquals(obj::class, obj::memberFun.container)
    assertEquals(obj::class, obj::memberVal.container)
    assertEquals(obj::class, obj::memberVar.container)
    assertEquals(obj::class, obj::delegated.container)
    assertEquals(obj::class, obj.delegated)

    class LocalClass {
        fun memberFun() {}
        fun String.memberExtFun() {}
        val memberVal: String = ""
        val String.memberExtVal: String get() = ""
        var memberVar: String = ""
        val delegated by DelegateExposeContainer()

        companion {
            fun blockFun() {}
            val blockVal = ""
            var blockVar = 0
            val blockDelegated by DelegateExposeContainer()
        }
    }
    assertEquals(LocalClass::class, LocalClass::memberFun.container)
    assertEquals(LocalClass::class, LocalClass::memberVal.container)
    assertEquals(LocalClass::class, LocalClass::memberVar.container)
    assertEquals(LocalClass::class, LocalClass::delegated.container)
    assertEquals(LocalClass::class, LocalClass().delegated)
    assertEquals(LocalClass::class, LocalClass::blockFun.container)
    assertEquals(LocalClass::class, LocalClass::blockVal.container)
    assertEquals(LocalClass::class, LocalClass::blockVar.container)
    assertEquals(LocalClass::class, LocalClass::blockDelegated.container)
    assertEquals(LocalClass::class, LocalClass.blockDelegated)

    return "OK"
}
