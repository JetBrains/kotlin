import org.jetbrains.annotations.NotNull;

interface Named {
    @NotNull
    String getName();
}

interface NamedEx extends Named {

}