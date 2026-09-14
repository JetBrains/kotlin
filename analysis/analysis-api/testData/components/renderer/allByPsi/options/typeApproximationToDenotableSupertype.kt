// TYPE_APPROXIMATION: TO_DENOTABLE_SUPERTYPE
// FILE: main.kt
fun flexible(j: JavaDeclaration) = j.value()

fun flexibleList(j: JavaDeclaration) = j.list()

// FILE: JavaDeclaration.java
import java.util.List;

public interface JavaDeclaration {
    String value();

    List<String> list();
}
