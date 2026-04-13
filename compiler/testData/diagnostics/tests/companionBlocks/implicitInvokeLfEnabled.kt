// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: J.java
public class J {
    public static boolean invoke() { return false; }
    public static String invoke(String s) { return s; }

    public static final Invokable invo = new Invokable();
}

// FILE: test.kt
class Invokable {
    operator fun invoke() {}
}

class C {
    companion {
        operator fun invoke() = false
        operator fun invoke(s: String) = s

        val invo = Invokable()
    }
}

class C2 {
    companion {
        operator fun invoke() = false
        operator fun invoke(s: String) = s
    }

    companion object {
        operator fun invoke(s: String) = null
        operator fun invoke(i: Int) = i

        val invo = Invokable()
    }
}

fun testC() {
    val c: C = C()
    val s: String = C("")

    C.invo()
}

fun testC2() {
    val c: C2 = C2()
    val s: String = C2("")
    val i: Int = C2(1)

    C2.invo()
}

fun testJ() {
    val j: J = J()
    val s: String = J("")

    J.invo()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, flexibleType, functionDeclaration, integerLiteral,
javaFunction, javaProperty, javaType, localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral */
