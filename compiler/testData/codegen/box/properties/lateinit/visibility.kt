// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK

import java.lang.reflect.Modifier

public class A {
    private lateinit var privateField: String
    protected lateinit var protectedField: String
    public lateinit var publicField: String

    fun test(): String {
        val clazz = A::class.java
        val cond = arrayListOf<String>()

        if (!Modifier.isPrivate(clazz.getDeclaredField("privateField").modifiers)) cond += "NOT_PRIVATE"
        if (!Modifier.isProtected(clazz.getDeclaredField("protectedField").modifiers)) cond += "NOT_PROTECTED"
        if (!Modifier.isPublic(clazz.getDeclaredField("publicField").modifiers)) cond += "NOT_PUBLIC"

        try {
            val a = privateField
        } catch (e: UninitializedPropertyAccessException) {
            return if (cond.isEmpty()) "OK" else cond.joinToString()
        }

        return "EXCEPTION WAS NOT CAUGHT"
    }
}

fun box(): String {
    return A().test()
}
