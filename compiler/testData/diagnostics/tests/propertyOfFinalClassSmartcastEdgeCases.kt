// RUN_PIPELINE_TILL: FRONTEND
// FILE: Id.java
public class Id {
    public static <T> T id(T it) {
        return it;
    }
}

// FILE: JavaConstantHolder.java
public class JavaConstantHolder {
    public static String someFlexible;
}

// FILE: Kotlin.kt
import kotlin.contracts.*

class KotlinType

data class LocalClass(val type: KotlinType)

abstract class ConstantValue<out T>(open val value: T)

class KClassValue(value: Any) : ConstantValue<Any>(value) {
    fun testClassLike() {
        if (value is LocalClass) {
            val type: KotlinType = value.type
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T> hihiEquals(flexible: T, other: Any): Boolean {
    contract {
        returns(true) implies (other is T)
    }
    return other is T
}

open class OpenKClassValue(value: Any) : ConstantValue<Any>(value) {
    class FinalSubOfOpenKClassValue : OpenKClassValue(Any())

    inline fun <reified T : FinalSubOfOpenKClassValue?> testTypeParameter(value: Any) {
        if (this is T) {
            if (value is LocalClass) {
                val type: KotlinType = value.type
            }
        }
    }

    inline fun <reified T : FinalSubOfOpenKClassValue?> testDnn() {
        if (this is <!CANNOT_CHECK_FOR_ERASED!>(T & Any)<!>) {
            if (value is LocalClass) {
                val type: KotlinType = value.type
            }
        }
    }

    fun testFlexible() {
        if (!hihiEquals(JavaConstantHolder.someFlexible, this)) return

        if (value is LocalClass) {
            val type: KotlinType = value.type
        }
    }
}

interface ConstantValueInterface<out T> {
    val value: T
}

interface InterfacedKClassValue : ConstantValueInterface<Any> {
    class MyClass

    fun testIntersection() {
        if (<!IMPOSSIBLE_IS_CHECK_ERROR!>this is MyClass<!>) {
            if (value is LocalClass) {
                val type: KotlinType = value.type
            }
        }
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, data, functionDeclaration, ifExpression, inline,
interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, reified, smartcast, typeConstraint, typeParameter */
