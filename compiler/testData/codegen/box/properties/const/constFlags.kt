// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FULL_JDK

@file:JvmName("XYZ")
import java.lang.reflect.Modifier

private const val privateConst: Int = 1
public const val publicConst: Int = 3

public object A {
    private const val privateConst: Int = 1
    public const val publicConst: Int = 3
}

public class B {
    companion object {
        private const val privateConst: Int = 1
        protected const val protectedConst: Int = 2
        public const val publicConst: Int = 3
    }
}

fun check(clazz: Class<*>, expectProtected: Boolean = true) {
    val fields = clazz.declaredFields.filter { it.name.contains("Const") }

    assert(fields.all { Modifier.isStatic(it.modifiers) }) { "`$clazz` contains non-static fields" }

    assert(Modifier.isPrivate(fields.single { it.name.contains("private") }.modifiers)) {
        "`$clazz`.privateConst is not private"
    }

    assert(Modifier.isPublic(fields.single { it.name.contains("public") }.modifiers)) {
        "`$clazz`.publicConst is not public"
    }

    if (expectProtected) {
        assert(Modifier.isProtected(fields.single { it.name.contains("protected") }.modifiers)) {
            "`$clazz`.protectedConst is not protected"
        }
    }
}

fun box(): String {
    check(A::class.java, false)
    check(B::class.java)
    check(Class.forName("XYZ"), false)

    return "OK"
}
