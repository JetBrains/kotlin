import org.jetbrains.annotations.NotNull;

public class ClassTypeParameterBoundWithWarnings <T extends @NotNull String> {
    ClassTypeParameterBoundWithWarnings() { }
    ClassTypeParameterBoundWithWarnings(T x) { }
}
