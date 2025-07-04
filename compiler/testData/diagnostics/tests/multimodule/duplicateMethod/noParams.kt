// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNNECESSARY_SAFE_CALL -SAFE_CALL_WILL_CHANGE_NULLABILITY

// MODULE: m1
// FILE: a.kt
package p

public interface B {
    public fun getParent(): B?
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public class C : B {
    override fun getParent(): B? = null

}

// MODULE: m3
// FILE: b.kt
package p

public interface B {
    public fun getParent(): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C) {
        b?.getParent()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, override, safeCall, smartcast */
