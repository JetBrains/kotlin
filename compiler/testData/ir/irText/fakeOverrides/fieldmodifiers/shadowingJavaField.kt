// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: Deserialization synthesizes fake overrides for Java superclass fields during KLIB linking.

// FILE: BaseJava.java
public class BaseJava {
    public String foo = "java_base_foo";
    public static String staticFoo = "java_static_foo";
    protected int bar = 42;
}

// FILE: BaseJavaGeneric.java
public class BaseJavaGeneric<T> {
    public T genericField;
}

// FILE: 1.kt
import kotlin.jvm.JvmField

// 1. Basic @JvmField var shadowing
class KtSubVar : BaseJava() {
    @JvmField
    var foo: String = "kt_sub_var_foo"
}

// 2. @JvmField val (read-only) shadowing
class KtSubVal : BaseJava() {
    @JvmField
    val foo: String = "kt_sub_val_foo"
}

// 3. Custom getter/setter (non-@JvmField) shadowing Java field
class KtSubCustomAccessor : BaseJava() {
    var foo: String
        get() = "custom_getter"
        set(value) {}
}

// 4. Protected field shadowing
class KtSubProtected : BaseJava() {
    @JvmField
    protected var bar: Int = 100
}

// 5. Generic field shadowing
class KtSubGeneric : BaseJavaGeneric<String>() {
    @JvmField
    var genericField: String = "generic_shadow"
}

// 6. Extension property with same name (should NOT conflict)
val BaseJava.foo: String
    get() = "extension_foo"

fun test(
    subVar: KtSubVar,
    subVal: KtSubVal,
    subCustom: KtSubCustomAccessor,
    subGeneric: KtSubGeneric,
    base: BaseJava
) {
    subVar.foo
    subVal.foo
    subCustom.foo
    subGeneric.genericField
    base.foo
}
