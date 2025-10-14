// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

// FILE: JBase.java
public class JBase {
    private final int aProp;
    private boolean active;
    protected int count;
    private String name;
    private boolean ready;

    public JBase(int aProp, boolean active, int count, String name, boolean ready) {
        this.aProp = aProp;
        this.active = active;
        this.count = count;
        this.name = name;
        this.ready = ready;
    }

    public int getAProp() { return aProp; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}

// FILE: JChild.java
public class JChild extends JBase {
    public JChild(int aProp, boolean active, int count, String name, boolean ready) {
        super(aProp, active, count, name, ready);
    }

    @Override public int getAProp()   { return super.getAProp() * 10; }
    @Override public boolean isActive(){ return !super.isActive(); }
    @Override public int getCount()   { return super.getCount() + 1; }
}

// FILE: JFieldsOnly.java
public class JFieldsOnly {
    public int fCode;
    public String fName;
    public boolean fEnabled;

    public JFieldsOnly(int code, String name, boolean enabled) {
        this.fCode = code;
        this.fName = name;
        this.fEnabled = enabled;
    }
}

// FILE: JavaInteropPositive.kt

val jBase  = JBase(1, true, 2, "base", true)
val jChild = JChild(1, true, 2, "child", false)
val jFields = JFieldsOnly(7, "fields", true)

inline fun consumeJBase(block: (JBase) -> Unit) = block(jBase)
inline fun consumeJFields(block: (JFieldsOnly) -> Unit) = block(jFields)

fun javaOverridePositive() {
    (val aProp, val isActive, val count, val name, val isReady) = jChild
    aProp.inv()
    isActive.not()
    count.inv()
    name.substring(1)
    isReady.not()
}

fun javaFieldsVisiblePositive() {
    (val fCode, val fName, val fEnabled) = jFields
    fCode.inv()
    fName.substring(1)
    fEnabled.not()

    (val code = fCode, val enabled = fEnabled) = jFields
    code.inv()
    enabled.not()
}

fun javaSyntheticPropsPositive() {
    (val aProp, val isActive, val count, val name, val isReady) = jBase
    aProp.inv()
    isActive.not()
    count.inv()
    name.substring(1)
    isReady.not()

    (val c = count, val n = name, val active = isActive) = jBase
    c.inv()
    n.substring(1)
    active.not()
}

fun javaInteropLambdaParamsPositive() {
    consumeJBase { (val name, val isReady, val count) -> Unit }
    jBase.let { (val aProp, val isActive) -> Unit }

    consumeJFields { (val fName, val fEnabled) -> Unit }
    jFields.let { (val code = fCode, val enabled = fEnabled) -> Unit }
}

fun javaInteropForLoopPositive() {
    for ((val name, val isReady) in listOf(jBase, jChild)) { }
    for ((val fCode, val fName) in listOf(jFields)) { }
}

fun destructureJavaSizePositive(list: java.util.ArrayList<String>) {
    (val size) = list
    size.inv()

    (val length = size) = list
    length.inv()

    list.let { (val size) -> size.inv() }
}

fun destructureJavaSizeNullabilityPositive(list: java.util.ArrayList<String>?) {
    (val size) = list ?: return
    size.inv()
}

fun destructureJavaSizeForPositive(lists: List<java.util.ArrayList<Int>>) {
    for ((val size) in lists) {
        size.inv()
    }
}

enum class EnumInterop {
    X, Y;
    val foo get() = ""
}

fun destructuringJavaEnumPositive() {
    (val name, val ordinal, val foo) = EnumInterop.X
    name.substring(0)
    ordinal.inv()
    foo.substring(0)

    (val n = name, val idx = ordinal, val f = foo) = EnumInterop.Y
    n.substring(0)
    idx.inv()
    f.substring(0)
}

fun destructuringJavaEnumLambdaPositive() {
    EnumInterop.X.let { (val name, val ordinal) ->
        name.substring(0)
        ordinal.inv()
    }
}

fun destructuringJavaEnumForPositive() {
    for ((val name, val ordinal) in EnumInterop.values()) {
        name.substring(0)
        ordinal.inv()
    }
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, elvisExpression, enumDeclaration, enumEntry, flexibleType, forLoop,
functionDeclaration, functionalType, getter, inline, integerLiteral, javaFunction, javaProperty, javaType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral */
