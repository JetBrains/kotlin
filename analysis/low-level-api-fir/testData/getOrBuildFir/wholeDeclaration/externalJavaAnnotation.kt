// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: usage.kt
<expr>
fun test() = ClassWithExternalAnnotatedMembers().externalNotNullMethod()
</expr>

// FILE: ClassWithExternalAnnotatedMembers.java
public class ClassWithExternalAnnotatedMembers {
    public String externalNotNullMethod() {
        return "";
    }
}

// FILE: annotations.xml
<root>
    <item name='ClassWithExternalAnnotatedMembers java.lang.String externalNotNullMethod()'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>