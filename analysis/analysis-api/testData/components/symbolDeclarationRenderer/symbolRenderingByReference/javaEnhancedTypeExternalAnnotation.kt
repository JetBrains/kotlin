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



// FILE: main.kt
import ClassWithExternalAnnotatedMembers

fun test(a: ClassWithExternalAnnotatedMembers) = a.e<caret><caret_onAirContext>xternalNotNullMethod()