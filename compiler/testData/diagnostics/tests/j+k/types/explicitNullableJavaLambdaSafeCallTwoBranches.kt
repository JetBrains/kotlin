// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: Fun.java
public interface Fun<T> {
    T run();
}

// FILE: Use.java
public class Use {
    public static <T> T eval(Fun<T> f) {
        return f.run();
    }
}

// FILE: Test.kt
fun <R> nullableMaterialize(): R? = null
fun <I, O> I.exec(block: I.() -> O): O = block(this)

fun test(x: String?) {
    val a = Use.eval<String?> {
        x?.exec {
            if (length > 0) this
            else null
        }
    }

    val b = Use.eval<String> {
        x?.exec {
            if (length > 0) "ok"
            else "fallback"
        } ?: "fallback"
    }

    val c: String? = a
    val d: String = b

    val bad: String = a
}

/* GENERATED_FIR_TAGS: equalityOperator, flexibleType, ifExpression, javaFunction, javaType,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, stringLiteral */