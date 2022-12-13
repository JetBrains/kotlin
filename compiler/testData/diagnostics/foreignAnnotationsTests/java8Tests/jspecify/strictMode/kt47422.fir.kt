// JSPECIFY_STATE: strict
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: Foo.java
import org.jspecify.annotations.*;

public interface Foo<T extends @Nullable Object> {}

// FILE: Util.java
public class Util {
    public static Foo<String> getFooOfString() {
        throw new RuntimeException();
    }
}

// FILE: UtilNullMarked.java
import org.jspecify.annotations.*;

@NullMarked
public class UtilNullMarked {
    public static Foo<String> getFooOfString() {
        throw new RuntimeException();
    }
}

// FILE: UtilNullMarkedGeneric.java
import org.jspecify.annotations.*;

@NullMarked
public class UtilNullMarkedGeneric {
    public static <K> Foo<K> getFooOfK() {
        throw new RuntimeException();
    }
}

// FILE: UtilNullMarkedGenericNullableBound.java
import org.jspecify.annotations.*;

@NullMarked
public class UtilNullMarkedGenericNullableBound {
    public static <K extends @Nullable Object> Foo<K> getFooOfK() {
        throw new RuntimeException();
    }
}

// FILE: UtilGenericNullableBound.java
import org.jspecify.annotations.*;

public class UtilGenericNullableBound {
    public static <K extends @Nullable Object> Foo<K> getFooOfK() {
        throw new RuntimeException();
    }
}

// FILE: UtilNullMarkedGenericNullnessUnspecifiedBound.java
import org.jspecify.annotations.*;

@NullMarked
public class UtilNullMarkedGenericNullnessUnspecifiedBound {
    public static <K extends @NullnessUnspecified Object> Foo<K> getFooOfK() {
        throw new RuntimeException();
    }
}

// FILE: UtilGenericNullnessUnspecifiedBound.java
import org.jspecify.annotations.*;

public class UtilGenericNullnessUnspecifiedBound {
    public static <K extends @NullnessUnspecified Object> Foo<K> getFooOfK() {
        throw new RuntimeException();
    }
}

// FILE: main.kt

// no errors on this call means String in Foo is flexible
fun isNotNullAndNullableStringInFoo(x: Foo<String>, y: Foo<String?>) {}

fun test1() {
    // String in Foo is flexible
    isNotNullAndNullableStringInFoo(
        Util.getFooOfString(),
        Util.getFooOfString()
    )
}

fun test2() {
    // String in Foo is not null
    isNotNullAndNullableStringInFoo(
        UtilNullMarked.getFooOfString(),
        // jspecify_nullness_mismatch
        <!ARGUMENT_TYPE_MISMATCH!>UtilNullMarked.getFooOfString()<!>
    )
}

fun test3() {
    // String in Foo is not null
    isNotNullAndNullableStringInFoo(
        UtilNullMarkedGeneric.getFooOfK(),
        // jspecify_nullness_mismatch
        <!ARGUMENT_TYPE_MISMATCH!>UtilNullMarkedGeneric.getFooOfK()<!>
    )
}

fun test4() {
    // String in Foo is flexible
    isNotNullAndNullableStringInFoo(
        UtilNullMarkedGenericNullableBound.getFooOfK(),
        UtilNullMarkedGenericNullableBound.getFooOfK()
    )
}

fun test5() {
    // String in Foo is flexible
    isNotNullAndNullableStringInFoo(
        UtilGenericNullableBound.getFooOfK(),
        UtilGenericNullableBound.getFooOfK()
    )
}

fun test6() {
    // String in Foo is flexible
    isNotNullAndNullableStringInFoo(
        UtilNullMarkedGenericNullnessUnspecifiedBound.getFooOfK(),
        UtilNullMarkedGenericNullnessUnspecifiedBound.getFooOfK()
    )
}

fun test7() {
    // String in Foo is flexible
    isNotNullAndNullableStringInFoo(
        UtilGenericNullnessUnspecifiedBound.getFooOfK(),
        UtilGenericNullnessUnspecifiedBound.getFooOfK()
    )
}
