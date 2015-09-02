import java.lang.reflect.Modifier

public class A {
    private lateinit val privateField: String
    protected lateinit var protectedField: String
    public lateinit var publicField: String

    fun test(): String {
        val clazz = A::class.java
        val cond = arrayListOf<String>()

        if (!Modifier.isPrivate(clazz.getDeclaredField("privateField").modifiers)) cond += "NOT_PRIVATE"
        if (!Modifier.isProtected(clazz.getDeclaredField("protectedField").modifiers)) cond += "NOT_PROTECTED"
        if (!Modifier.isPublic(clazz.getDeclaredField("publicField").modifiers)) cond += "NOT_PUBLIC"

        if (Modifier.isPrivate(clazz.getDeclaredField("privateField").modifiers)) cond += "FINAL"

        return if (cond.isEmpty()) "OK" else cond.joinToString()
    }
}

fun box(): String {
    return A().test()
}