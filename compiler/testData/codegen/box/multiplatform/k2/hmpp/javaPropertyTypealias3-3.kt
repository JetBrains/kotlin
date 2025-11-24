// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class LibClass1
expect class LibClass2
expect class LibClass3

expect fun libClass1Value(x: LibClass1): String
expect fun libClass2Value(x: LibClass2): String
expect fun libClass3Value(x: LibClass3): String

val LibClass1.value: String get() = libClass1Value(this)
val LibClass2.value: String get() = libClass2Value(this)
val LibClass3.value: String get() = libClass3Value(this)


// MODULE: lib-inter()()(lib-common)
expect class LibInterClass1

expect fun libInterClass1Value(x: LibInterClass1): String

actual class LibClass2(
    val value: String = "2"
)

actual fun libClass2Value(x: LibClass2): String = x.value

actual typealias LibClass3 = LibInterClass1


// MODULE: lib-platform()()(lib-inter)
// FILE: LibJava1.java
public class LibJava1 {
    public final String value;

    public LibJava1(String value) {
        this.value = value;
    }
}

// FILE: LibJava3.java
public class LibJava3 {
    public final String value;

    public LibJava3(String value) {
        this.value = value;
    }
}

// FILE: libPlatform.kt
actual typealias LibClass1 = LibJava1
actual typealias LibInterClass1 = LibJava3

actual fun libClass1Value(x: LibClass1): String = x.value
actual fun libInterClass1Value(x: LibInterClass1): String = x.value

actual fun libClass3Value(x: LibClass3): String = libInterClass1Value(x)


// MODULE: app-common(lib-common)
fun testCommon(
    lc1: LibClass1,
    lc2: LibClass2,
    lc3: LibClass3,
): String {
    return lc1.value + lc2.value + lc3.value
}

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun testInter(
    lc2: LibClass2,
    lc3: LibClass3,
    lic1: LibInterClass1
): String {
    return lc2.value + lc3.value + libInterClass1Value(lic1)
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun testPlatform(
    lc1: LibClass1,
    lc2: LibClass2,
    lc3: LibClass3,
    lic1: LibInterClass1
): String {
    return lc1.value + lc2.value + lc3.value + libInterClass1Value(lic1)
}

fun box(): String {
    val lc1 = LibClass1("1")
    val lc2 = LibClass2()
    val lic1 = LibInterClass1("3")
    val lc3: LibClass3 = lic1

    val fromCommon = testCommon(lc1, lc2, lc3)
    if (fromCommon != "123") {
        return "Fail"
    }

    val fromInter = testInter(lc2, lc3, lic1)
    if (fromInter != "233") {
        return "Fail"
    }

    val fromPlatform = testPlatform(lc1, lc2, lc3, lic1)
    if (fromPlatform != "1233") {
        return "Fail"
    }

    return "OK"
}
