// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

//FILE: JBase.java
public class JBase {
    private final int aProp;
    private final boolean active;
    protected final int count;
    private final String name;
    private final boolean ready;

    public JBase(int aProp, boolean active, int count, String name, boolean ready) {
        this.aProp = aProp;
        this.active = active;
        this.count = count;
        this.name = name;
        this.ready = ready;
    }

    public int getAProp() { return aProp; }
    public boolean isActive() { return active; }
    public int getCount() { return count; }
    public String getName() { return name; }
    public boolean isReady() { return ready; }
}

//FILE: JChildWithExtra.java
public class JChildWithExtra extends JBase {
    public JChildWithExtra(int aProp, boolean active, int count, String name, boolean ready) {
        super(aProp, active, count, name, ready);
    }

    public int getChildOnly() { return 42; }
}

//FILE: JFieldsOnly.java
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

//FILE: JFieldsMixed.java
public class JFieldsMixed {
    public int ok;
    private int secret;
    public static int staticField = 7;

    public JFieldsMixed(int ok, int secret) {
        this.ok = ok;
        this.secret = secret;
    }
}

//FILE: JWeirdAccessors.java
public class JWeirdAccessors {
    public JWeirdAccessors() {}

    public boolean hasReady() { return true; }
    public void getVoid() {}
    public int getNumber(int k) { return k; }
    public static int getStaticNumber() { return 5; }
    public boolean getEnabled() { return true; }
}

//FILE: JavaInteropNegative.kt
val jBase  = JBase(1, true, 2, "base", true)
val jChildWithExtra = JChildWithExtra(1, true, 2, "childExtra", true)

val jFields = JFieldsOnly(7, "fields", true)
val jMixedFields = JFieldsMixed(10, 99)

val jWeird = JWeirdAccessors()

inline fun consumeJBase(block: (JBase) -> Unit) = block(jBase)

enum class EnumInterop { X, Y }

fun javaOverrideStaticTypeNegative() {
    val asBase: JBase = jChildWithExtra
    val (<!UNRESOLVED_REFERENCE!>childOnly<!>) = asBase
}

fun javaFieldsVisibleNegative() {
    val (
        <!UNRESOLVED_REFERENCE!>code<!>,
    <!UNRESOLVED_REFERENCE!>name<!>,
    <!UNRESOLVED_REFERENCE!>enabled<!>
    ) = jFields

    val (
        <!INVISIBLE_REFERENCE!>secret<!>,
    <!UNRESOLVED_REFERENCE!>staticField<!>
    ) = jMixedFields
}

fun javaSyntheticPropsNegative() {
    val (
        <!FUNCTION_CALL_EXPECTED!>hasReady<!>,
    <!UNRESOLVED_REFERENCE!>`void`<!>,
    <!UNRESOLVED_REFERENCE!>number<!>,
    <!UNRESOLVED_REFERENCE!>staticNumber<!>,
    <!UNRESOLVED_REFERENCE!>isEnabled<!>
    ) = jWeird
}

fun javaRenameAndDuplicatesNegative() {
    val (
        <!REDECLARATION!>x<!> = name,
    <!REDECLARATION!>x<!> = count,
    y = <!UNRESOLVED_REFERENCE!>unknown<!>
    ) = jBase

    val (
        <!REDECLARATION!>name<!>,
    <!REDECLARATION!>name<!>
    ) = jBase
}

fun javaInteropLambdaParamsNegative() {
    consumeJBase { (
        <!UNRESOLVED_REFERENCE!>unknown<!>,
        <!UNRESOLVED_REFERENCE!>isEnabled<!>
        ) -> Unit }

    jBase.let { (
        <!REDECLARATION!>name<!>,
        <!REDECLARATION!>name<!>
        ) -> Unit }

    consumeJBase { (
        <!REDECLARATION!>x<!> = count,
        <!REDECLARATION!>x<!> = aProp
        ) -> Unit }
}

fun javaInteropForLoopNegative() {
    for ((code, staticField) in <!ITERATOR_MISSING!><!UNRESOLVED_REFERENCE!>listOf<!>(jFields, jFields)<!>) { }

    val listAsBase: List<JBase> = <!UNRESOLVED_REFERENCE!>listOf<!>(jChildWithExtra)
    for ((<!UNRESOLVED_REFERENCE!>childOnly<!>) in listAsBase) { }
}

fun destructuringJavaListMethodNegative() {
    val list = java.util.ArrayList<String>()
    val (bad = <!FUNCTION_CALL_EXPECTED!>clear<!>) = list
}

fun destructuringJavaEnumNegative() {
    val (<!UNRESOLVED_REFERENCE!>nope<!>) = EnumInterop.X
    val (id = <!UNRESOLVED_REFERENCE!>namee<!>) = EnumInterop.X
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, enumDeclaration, enumEntry, flexibleType, forLoop, functionDeclaration,
functionalType, inline, integerLiteral, javaFunction, javaProperty, javaType, lambdaLiteral, localProperty,
propertyDeclaration, smartcast, stringLiteral */
