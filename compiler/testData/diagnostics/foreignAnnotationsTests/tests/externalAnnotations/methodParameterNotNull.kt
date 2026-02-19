// FILE: ClassWithExternalAnnotatedMembers.java
import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    public void method(String s) {
    }

    public void method(@NotNull Integer i) {
    }
}

// FILE: usage.kt
fun test() {
    val instance = ClassWithExternalAnnotatedMembers()
    val i: Int? = null
    instance.method(<!TYPE_MISMATCH!>i<!>)

    val s: String? = null
    instance.method(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>s<!>)

    val b: Boolean? = null
    instance.<!NONE_APPLICABLE!>method<!>(b)

    instance.method(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}

// FILE: annotations.xml
<root>
    <item name='ClassWithExternalAnnotatedMembers void method(java.lang.String) 0'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>