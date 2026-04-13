import org.jetbrains.annotations.NotNull;
import java.util.List;

public class J {
    public static String nullabilityFlexible() { return null; }

    @NotNull
    public static List<String> mutabilityFlexible() { return null; }

    public static List<String> bothFlexible() { return null; }
}