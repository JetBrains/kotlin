// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
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
    val (aProp, isActive, count, name, isReady) = jChild
    aProp.inv()
    isActive.not()
    count.inv()
    name.substring(1)
    isReady.not()
}

fun javaFieldsVisiblePositive() {
    val (fCode, fName, fEnabled) = jFields
    fCode.inv()
    fName.substring(1)
    fEnabled.not()

    val (code = fCode, enabled = fEnabled) = jFields
    code.inv()
    enabled.not()
}

fun javaSyntheticPropsPositive() {
    val (aProp, isActive, count, name, isReady) = jBase
    aProp.inv()
    isActive.not()
    count.inv()
    name.substring(1)
    isReady.not()

    val (c = count, n = name, active = isActive) = jBase
    c.inv()
    n.substring(1)
    active.not()
}

fun javaInteropLambdaParamsPositive() {
    consumeJBase { (name, isReady, count) -> Unit }
    jBase.let { (aProp, isActive) -> Unit }

    consumeJFields { (fName, fEnabled) -> Unit }
    jFields.let { (code = fCode, enabled = fEnabled) -> Unit }
}

fun javaInteropForLoopPositive() {
    for ((name, isReady) in listOf(jBase, jChild)) { }
    for ((fCode, fName) in listOf(jFields)) { }
}

fun destructureJavaSizePositive(list: java.util.ArrayList<String>) {
    val (size) = list
    size.inv()

    val (length = size) = list
    length.inv()

    list.let { (size) -> size.inv() }
}

fun destructureJavaSizeNullabilityPositive(list: java.util.ArrayList<String>?) {
    val (size) = list ?: return
    size.inv()
}

fun destructureJavaSizeForPositive(lists: List<java.util.ArrayList<Int>>) {
    for ((size) in lists) {
        size.inv()
    }
}

enum class EnumInterop {
    X, Y;
    val foo get() = ""
}

fun destructuringJavaEnumPositive() {
    val (name, ordinal, foo) = EnumInterop.X
    name.substring(0)
    ordinal.inv()
    foo.substring(0)

    val (n = name, idx = ordinal, f = foo) = EnumInterop.Y
    n.substring(0)
    idx.inv()
    f.substring(0)
}

fun destructuringJavaEnumLambdaPositive() {
    EnumInterop.X.let { (name, ordinal) ->
        name.substring(0)
        ordinal.inv()
    }
}

fun destructuringJavaEnumForPositive() {
    for ((name, ordinal) in EnumInterop.values()) {
        name.substring(0)
        ordinal.inv()
    }
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, elvisExpression, enumDeclaration, enumEntry, flexibleType, forLoop,
functionDeclaration, functionalType, getter, inline, integerLiteral, javaFunction, javaProperty, javaType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral */
