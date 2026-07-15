// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: CLASS

// NB: This test is a minimized example of code from the mybatis-plus library.
// Kotlin support was added in https://github.com/baomidou/mybatis-plus/pull/7100.
// Be careful not to break behavior checked by this test.

// With -Xsam-conversions=class the wrapper class has no writeReplace() even though the SAM interface is Serializable, so SerializedLambda
// extraction is impossible. Code in mybatis-plus falls back to scanning the wrapper for the referenced KCallable.
//   1. Property reference (User::name): the CallableReference (KProperty) is held in a field of the generated $sam$ wrapper; name comes
//      from KProperty.name, owner from CallableReference.owner.jClass.
//   2. Reference to a Java getter (PermissionDO::getBizType): compiled into a metadata-less singleton implementing the SAM interface
//      directly. There's no SerializedLambda or KCallable anywhere, so there's no way to obtain the referenced method at runtime.
//      If either SerializedLambda or KCallable appears in the future, the test may fail and will need to be adapted, which would imply that
//      code in mybatis-plus can be adapted to support the new case too.

// FILE: SFunction.java
import java.io.Serializable;
import java.util.function.Function;

public interface SFunction<T, R> extends Function<T, R>, Serializable {
}

// FILE: PermissionDO.java
public class PermissionDO {

    private Integer bizType;
    private Long userId;

    public Integer getBizType() {
        return bizType;
    }

    public void setBizType(Integer bizType) {
        this.bizType = bizType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

// FILE: test.kt
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty

class User {
    var name: String? = null
    var roleId: Int? = null
}

class Account {
    var isActive: Boolean = false
}

fun <T, R> column(ref: SFunction<T, R>): SFunction<T, R> = ref

data class Resolved(val property: String, val owner: Class<*>)

fun resolve(func: SFunction<*, *>): Resolved {
    val callable = findCallable(func)
        ?: error("UNRESOLVABLE: ${func.javaClass} has no writeReplace() and holds no KCallable")
    val owner = ((callable as CallableReference).owner as ClassBasedDeclarationContainer).jClass
    val property =
        if (callable is KProperty<*>) callable.name
        else error("UNRESOLVABLE: KCallable is not a property: $callable")
    return Resolved(property, owner)
}

// The wrapper itself may be the CallableReference, or hold one in a field.
private fun findCallable(func: Any): KCallable<*>? {
    if (func is KCallable<*>) return func
    var clazz: Class<*>? = func.javaClass
    while (clazz != null) {
        for (field in clazz.declaredFields) {
            val value = try {
                field.isAccessible = true
                field.get(func)
            } catch (e: Throwable) {
                continue
            }
            if (value is KCallable<*>) return value
        }
        clazz = clazz.superclass
    }
    return null
}

private fun expect(expected: Resolved, actual: Resolved, case: String): String? =
    if (expected != actual) "$case: expected $expected but was $actual" else null

// Pins the current (unresolvable) emission for Java getter references in class mode.
private fun expectMetadataLessSingleton(func: SFunction<*, *>, case: String): String? {
    try {
        func.javaClass.getDeclaredMethod("writeReplace")
        return "$case: unexpectedly has writeReplace() — the reference may be resolvable now; upgrade this to a positive resolve() assertion"
    } catch (e: NoSuchMethodException) {}

    if (findCallable(func) != null) {
        return "$case: unexpectedly reaches a KCallable — the reference may be resolvable now; upgrade this to a positive resolve() assertion"
    }

    return null
}

fun box(): String {
    val nameRef = column(User::name)
    val user = User().also { it.name = "a"; it.roleId = 7 }
    if (nameRef.apply(user) != "a") return "FAIL: User::name.apply() = ${nameRef.apply(user)}"
    val getterRef = column(PermissionDO::getBizType)
    val permission = PermissionDO().also { it.bizType = 42 }
    if (getterRef.apply(permission) != 42) return "FAIL: PermissionDO::getBizType.apply() = ${getterRef.apply(permission)}"

    val failures = listOfNotNull(
        expect(Resolved("name", User::class.java), resolve(nameRef), "User::name"),
        expect(Resolved("roleId", User::class.java), resolve(column(User::roleId)), "User::roleId"),
        expect(Resolved("isActive", Account::class.java), resolve(column(Account::isActive)), "Account::isActive"),
        expectMetadataLessSingleton(getterRef, "PermissionDO::getBizType"),
        expectMetadataLessSingleton(column(PermissionDO::getUserId), "PermissionDO::getUserId"),
    )
    if (failures.isNotEmpty()) return "FAIL:\n" + failures.joinToString("\n")
    return "OK"
}
