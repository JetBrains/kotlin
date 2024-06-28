// FILE: ClassWithExternalAnnotatedMembers.java
import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    public ClassWithExternalAnnotatedMembers(Integer i) { // with external annotation on parameter

    }

    public ClassWithExternalAnnotatedMembers(@NotNull String s) {

    }

    public ClassWithExternalAnnotatedMembers(Boolean b) {

    }
}

// FILE: usage.kt
fun test() {
    val i: Int? = null
    ClassWithExternalAnnotatedMembers(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>i<!>)

    val s: String? = null
    <!NONE_APPLICABLE!>ClassWithExternalAnnotatedMembers<!>(s)

    val b: Boolean? = null
    ClassWithExternalAnnotatedMembers(b)

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ClassWithExternalAnnotatedMembers<!>(null)
}

// FILE: annotations.xml
<root>
    <item name='ClassWithExternalAnnotatedMembers ClassWithExternalAnnotatedMembers(java.lang.Integer) 0'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>