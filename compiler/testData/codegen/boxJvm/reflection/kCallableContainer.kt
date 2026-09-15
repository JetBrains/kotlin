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

class C {
    fun memberFun() {}
    fun String.memberExtFun() {}
    val memberVal = ""
    val String.memberExtVal get() = ""
    var memberVar = 0
    val memberLazyVal by lazy { 0 }

    companion {
        fun blockFun() {}
        val blockVal = ""
        var blockVar = 0
        val blockLazyVal by lazy { 0 }
    }

    companion object {
        fun companionObjectFun() {}
        fun String.companionObjectExtFun() {}
        val companionObjectVal = ""
        val String.companionObjectExtVal get() = ""
        var companionObjectVar = 0
        val companionObjectLazyVal by lazy { 0 }
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
//        val blockLazyVal by lazy { 0 }
    }

    companion object {
        fun companionObjectFun() {}
        fun String.companionObjectExtFun() {}
        val companionObjectVal = ""
        val String.companionObjectExtVal get() = ""
        var companionObjectVar = 0
        val companionObjectLazyVal by lazy { 0 }
    }
}

fun toplevelFun() {}
fun String.toplevelExtFun() {}
val toplevelVal = ""
val String.toplevelExtVal get() = ""
var toplevelVar = 0
val toplevelLazyVal by lazy { 0 }

class E

companion fun E.compExtFun() {}
companion val E.compExtVal get() = ""
companion var E.compExtVar get() = ""; set(value) {}
companion val E.compExtLazyVal by lazy { "" }

fun box(): String {
    J::class.members.forEach { assertEquals(it.container, J::class) }
    C::class.members.forEach { assertEquals(it.container, C::class) }
    I::class.members.forEach { assertEquals(it.container, I::class) }

    assertEquals(J::field.container, J::class)
    assertEquals(J::sField.container, J::class)
    assertEquals(J::foo.container, J::class)
    assertEquals(J::sFoo.container, J::class)

    assertEquals(C::memberFun.container, C::class)
    assertEquals(C::memberVal.container, C::class)
    assertEquals(C::memberVar.container, C::class)
    assertEquals(C::memberLazyVal.container, C::class)
    assertEquals(C::blockFun.container, C::class)
    assertEquals(C::blockVal.container, C::class)
    assertEquals(C::blockVar.container, C::class)
    assertEquals(C::blockLazyVal.container, C::class)
    assertEquals(C::companionObjectFun.container, C.Companion::class)
    assertEquals(C::companionObjectVal.container, C.Companion::class)
    assertEquals(C::companionObjectVar.container, C.Companion::class)
    assertEquals(C::companionObjectLazyVal.container, C.Companion::class)

    assertEquals(I::memberFun.container, I::class)
    assertEquals(I::memberVal.container, I::class)
    assertEquals(I::memberVar.container, I::class)
    assertEquals(I::blockFun.container, I::class)
// TODO: uncomment when KT-85853 is fixed
//    assertEquals(I::blockVal.container, I::class)
//    assertEquals(I::blockVar.container, I::class)
//    assertEquals(I::blockLazyVal.container, I::class)
    assertEquals(I::companionObjectFun.container, I.Companion::class)
    assertEquals(I::companionObjectVal.container, I.Companion::class)
    assertEquals(I::companionObjectVar.container, I.Companion::class)
    assertEquals(I::companionObjectLazyVal.container, I.Companion::class)

    val fileClass = ::box.container
    assertEquals(fileClass.toString(), "file class TestKt")

    assertEquals(::toplevelFun.container, fileClass)
    assertEquals(String::toplevelExtFun.container, fileClass)
    assertEquals(::toplevelVal.container, fileClass)
    assertEquals(String::toplevelExtVal.container, fileClass)
    assertEquals(::toplevelVar.container, fileClass)
    assertEquals(::toplevelLazyVal.container, fileClass)
    assertEquals(E::compExtFun.container, fileClass)
    assertEquals(E::compExtVal.container, fileClass)
    assertEquals(E::compExtVar.container, fileClass)
    assertEquals(E::compExtLazyVal.container, fileClass)

    return "OK"
}
