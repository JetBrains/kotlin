// FILE: main.kt
fun some() {
  JavaClass().f<caret>oo();
}
// FILE: JavaClass.java
public class JavaClass {
  public String foo() {};
}
// FILE: annotations.xml
<root>
  <item name='JavaClass java.lang.String foo()'>
    <annotation name='org.jetbrains.annotations.NotNull'/>
  </item>
</root>