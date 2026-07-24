// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY

// NB: This test is a minimized example of code from the mybatis-plus library.
// Kotlin support was added in https://github.com/baomidou/mybatis-plus/pull/7100.
// Be careful not to break behavior checked by this test.

// Java libraries (mybatis-plus, EasyExcel, column DSLs, ...) resolve a property/column from a serializable method reference by extracting
// java.lang.invoke.SerializedLambda through the synthetic writeReplace() and reading implMethodName. For Kotlin references the compiler
// emits synthetic adapter methods (e.g. "box$lambda$0", "enclosing$getBizType"), so the name must instead be recovered from what the
// compiler does preserve. This test pins the two invokedynamic emission shapes libraries depend on:
//   1. Property reference (User::name): serializable lambda whose captured args contain the KProperty; owner class is recoverable from
//      SerializedLambda.instantiatedMethodType.
//   2. Reference to a Java getter (PermissionDO::getBizType): no KProperty is captured; the getter name survives only as the last
//      '$'-segment of the synthetic implMethodName.

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
import java.lang.invoke.SerializedLambda
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
    val writeReplace = func.javaClass.getDeclaredMethod("writeReplace")
    writeReplace.isAccessible = true
    val lambda = writeReplace.invoke(func) as SerializedLambda

    // Shape 1: property reference — the KProperty is captured by the synthetic lambda.
    for (i in 0 until lambda.capturedArgCount) {
        val arg = lambda.getCapturedArg(i)
        if (arg is KProperty<*>) {
            return Resolved(arg.name, ownerOf(lambda, func.javaClass.classLoader))
        }
    }

    val impl = lambda.implMethodName
    // Shape 2: Java getter reference — synthetic adapter named "enclosing$getBizType"; the real getter name is the last '$'-segment.
    val dollar = impl.lastIndexOf('$')
    if (dollar > 0) {
        propertyOfGetter(impl.substring(dollar + 1))?.let {
            return Resolved(it, ownerOf(lambda, func.javaClass.classLoader))
        }
    }
    // Plain Java-style emission: implMethodName is the getter itself.
    propertyOfGetter(impl)?.let {
        return Resolved(it, ownerOf(lambda, func.javaClass.classLoader))
    }
    error("UNRESOLVABLE: implMethodName='$impl' of ${lambda.implClass} carries no property name and no KProperty is captured")
}

private fun propertyOfGetter(name: String): String? = when {
    name.length > 3 && name.startsWith("get") && name[3].isUpperCase() ->
        name[3].lowercaseChar() + name.substring(4)
    name.length > 2 && name.startsWith("is") && name[2].isUpperCase() ->
        name
    else -> null
}

// Owner comes from instantiatedMethodType, e.g. "(LUser;)Ljava/lang/String;" -> User.
private fun ownerOf(lambda: SerializedLambda, classLoader: ClassLoader?): Class<*> {
    val type = lambda.instantiatedMethodType
    val name = type.substring(2, type.indexOf(';')).replace('/', '.')
    return Class.forName(name, false, classLoader)
}

private fun expect(expected: Resolved, actual: Resolved, case: String): String? =
    if (expected != actual) "$case: expected $expected but was $actual" else null

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
        expect(Resolved("bizType", PermissionDO::class.java), resolve(getterRef), "PermissionDO::getBizType"),
        expect(Resolved("userId", PermissionDO::class.java), resolve(column(PermissionDO::getUserId)), "PermissionDO::getUserId"),
    )
    if (failures.isNotEmpty()) return "FAIL:\n" + failures.joinToString("\n")
    return "OK"
}
