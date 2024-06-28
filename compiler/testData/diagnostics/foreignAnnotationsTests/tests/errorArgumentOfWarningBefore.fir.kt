// NULLABILITY_ANNOTATIONS: @io.reactivex.rxjava3.annotations:strict, @org.eclipse.jdt.annotation:warn
// LANGUAGE: -SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced
// SOURCE_RETENTION_ANNOTATIONS
// MUTE_FOR_PSI_CLASS_FILES_READING
// ^because annotations don't allow type parameter use.
// ISSUE: KT-63209

// FILE: A1.java
import java.util.List;

public class A1 {
    @org.eclipse.jdt.annotation.Nullable
    public static List<@io.reactivex.rxjava3.annotations.Nullable String> warningError() {
        return null;
    }

    @org.eclipse.jdt.annotation.Nullable
    public static List<@io.reactivex.rxjava3.annotations.Nullable List<@io.reactivex.rxjava3.annotations.Nullable String>> warningErrorError() {
        return null;
    }

    @org.eclipse.jdt.annotation.Nullable
    public static List<@org.eclipse.jdt.annotation.Nullable List<@io.reactivex.rxjava3.annotations.Nullable String>> warningWarningError() {
        return null;
    }

    @org.eclipse.jdt.annotation.Nullable
    public static List<@io.reactivex.rxjava3.annotations.Nullable List<@org.eclipse.jdt.annotation.Nullable String>> warningErrorWarning() {
        return null;
    }

    @org.eclipse.jdt.annotation.Nullable
    public static List<List<@io.reactivex.rxjava3.annotations.Nullable String>> warningPlatformError() {
        return null;
    }
}

// FILE: main.kt
fun main1() {
    val list = A1.warningError()
    val element = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("kotlin.collections.(Mutable)List<@Nullable() kotlin.String?>?")!>list<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("@Nullable() kotlin.String?;  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element<!>.length
}

fun main2() {
    val list = A1.warningErrorError()
    val element = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("kotlin.collections.(Mutable)List<(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?)>?")!>list<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?);  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("@Nullable() kotlin.String?;  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element!!.get(0)<!>.length
}

fun main3() {
    val list = A1.warningWarningError()
    val element = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("kotlin.collections.(Mutable)List<(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?)>?")!>list<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?)")!>element<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("@Nullable() kotlin.String?;  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element!!.get(0)<!>.length
}

fun main4() {
    val list = A1.warningErrorWarning()
    val element = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("kotlin.collections.(Mutable)List<(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?)>?")!>list<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("(@Nullable() kotlin.collections.MutableList<@Nullable() kotlin.String?>?..@Nullable() kotlin.collections.List<@Nullable() kotlin.String?>?);  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element<!>.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("@Nullable() kotlin.String?")!>element!!.get(0)<!>.length
}

fun main5() {
    val list = A1.warningPlatformError()
    val element = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("kotlin.collections.(Mutable)List<kotlin.collections.(Mutable)List<@Nullable() kotlin.String?>!>?")!>list<!>.get(0)
    element.get(0)
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS("@Nullable() kotlin.String?;  This will become an error in Kotlin 2.1. See https://youtrack.jetbrains.com/issue/KT-63209.")!>element!!.get(0)<!>.length
}
