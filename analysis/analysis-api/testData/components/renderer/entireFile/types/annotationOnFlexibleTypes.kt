// FILE: main.kt
fun annotatedString(j: JavaDeclaration) = j.annotatedString()

fun annotated(j: JavaDeclaration) = j.annotated("")

fun annotatedList(j: JavaDeclaration) = j.annotatedList()

fun annotatedMutableList(j: JavaDeclaration) = j.annotatedMutableList()

fun annotatedTypeParameter(j: JavaDeclaration) = j.annotatedTypeParameter<String>(null)

fun annotatedDefinitelyNotNull(j: JavaDeclaration) = j.annotatedDefinitelyNotNull<String?>(null)

fun <T> annotatedDefinitelyNotNullTypeParameter(j: JavaDeclaration, value: T) = j.annotatedDefinitelyNotNull(value)

// FILE: JavaDeclaration.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@Target(ElementType.TYPE_USE)
@interface Ann {}

public interface JavaDeclaration {
    @Ann
    String annotatedString();

    String annotated(@Ann String title);

    List<@Ann String> annotatedList();

    @Ann
    List<@Ann String> annotatedMutableList();

    <T> T annotatedTypeParameter(@Ann T value);

    <T> @Ann @NotNull T annotatedDefinitelyNotNull(T value);
}
