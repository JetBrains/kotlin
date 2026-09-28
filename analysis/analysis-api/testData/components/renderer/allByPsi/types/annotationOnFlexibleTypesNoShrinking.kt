// NO_FLEXIBLE_TYPE_SHRINKING
// FILE: main.kt
fun annotatedString(j: JavaDeclaration) = j.annotatedString()

fun annotatedList(j: JavaDeclaration) = j.annotatedList()

fun annotatedMutableList(j: JavaDeclaration) = j.annotatedMutableList()

// FILE: JavaDeclaration.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

@Target(ElementType.TYPE_USE)
@interface Ann {}

public interface JavaDeclaration {
    @Ann
    String annotatedString();

    List<@Ann String> annotatedList();

    @Ann
    List<@Ann String> annotatedMutableList();
}
