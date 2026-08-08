// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: MarkedClass.java

import org.chromium.build.annotations.*;

@NullMarked
public class MarkedClass {
    public String field = "";

    public MarkedClass(String arg) {}

    public String produce() { return ""; }

    public void consume(String arg) {}

    @Nullable
    public String produceNullable() { return null; }

    @NullUnmarked
    public String unmarkedProduce() { return ""; }

    @NullUnmarked
    public void unmarkedConsume(String arg) {}
}

// FILE: MarkedMembers.java

import org.chromium.build.annotations.*;

public class MarkedMembers {
    @NullMarked
    public String markedProduce() { return ""; }

    @NullMarked
    public void markedConsume(String arg) {}

    public String unannotatedProduce() { return ""; }

    public void unannotatedConsume(String arg) {}
}

// FILE: MarkedConstructor.java

import org.chromium.build.annotations.*;

public class MarkedConstructor {
    @NullMarked
    public MarkedConstructor(String arg) {}
}

// FILE: MarkedInterface.java

import org.chromium.build.annotations.*;

@NullMarked
public interface MarkedInterface {
    String produce();

    void consume(String arg);
}

// FILE: main.kt

class CorrectOverride : MarkedInterface {
    override fun produce(): String = ""
    override fun consume(arg: String) {}
}

class IncorrectOverride : MarkedInterface {
    <!WRONG_TYPE_FOR_JAVA_OVERRIDE!>override<!> fun produce(): String? = null
    <!WRONG_TYPE_FOR_JAVA_OVERRIDE!>override<!> fun consume(arg: String?) {}
}

fun markedClass(m: MarkedClass) {
    m.field.length
    m.produce().length
    m.consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    m.produceNullable()?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>m.produceNullable()<!>.length
    MarkedClass(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    m.unmarkedProduce().length
    m.unmarkedConsume(null)
}

fun markedMembers(m: MarkedMembers) {
    m.markedProduce().length
    m.markedConsume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    m.unannotatedProduce().length
    m.unannotatedConsume(null)
}

fun markedConstructor() {
    MarkedConstructor(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
