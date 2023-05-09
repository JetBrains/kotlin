// !CHECK_TYPE
// UNEXPECTED BEHAVIOUR
// ISSUES: KT-37066

// TESTCASE NUMBER: 1
// FILE: JavaClass.java
public final class JavaClass implements Comparable<JavaClass> {
    private final String name;

    public JavaClass (String name) {
        this.name = name;
    }

    @Override
    public int compareTo(JavaClass that) {
        return this.name.compareTo(that.name);
    }
}


// FILE: KotlinClass.kt
fun case1(javaClass: JavaClass?) {
    val validType: (JavaClass) -> Boolean = if (javaClass != null) { it -> it == javaClass } else BooCase1.FILTER

    val invalidType = if (javaClass != null) { it -> it == javaClass } else BooCase1.FILTER

    validType.checkType { _<Function1<JavaClass, Boolean>>() } //ok

    invalidType.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Function1<Nothing, Boolean>>() } //(!!!)

    Case1(javaClass).x.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Function1<Nothing, Boolean>>() } //(!!!)
}

class Case1(val javaClass: JavaClass?) {
    val x = if (javaClass != null) { it -> it == javaClass } else BooCase2.FILTER
}

class BooCase1() {
    companion object {
        val FILTER: (JavaClass) -> Boolean = { true }
    }
}

// TESTCASE NUMBER: 2

class KotlinClass(private val name: String) : Comparable<KotlinClass> {
    override operator fun compareTo(that: KotlinClass): Int {
        return name.compareTo(that.name)
    }
}

fun case2(kotlinClass: KotlinClass?) {
    val validType: (KotlinClass) -> Boolean = if (kotlinClass != null) { it -> it == kotlinClass } else BooCase2.FILTER
    val invalidType = if (kotlinClass != null) { it -> it == kotlinClass } else BooCase2.FILTER

    validType.checkType { _<Function1<KotlinClass, Boolean>>() } //ok

    invalidType.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Function1<Nothing, Boolean>>() }  //(!!!)

    Case2(kotlinClass).x.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Function1<Nothing, Boolean>>() } //(!!!)
}

class Case2(val kotlinClass: KotlinClass?) {
    val x = if (kotlinClass != null) { it -> it == kotlinClass } else BooCase2.FILTER
}

class BooCase2() {
    companion object {
        val FILTER: (KotlinClass) -> Boolean = { true }
    }
}
