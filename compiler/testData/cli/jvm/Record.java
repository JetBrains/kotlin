// This import checks that the compiler won't parse the "record" word as a soft keyword
// and load the non-existing class named "Unresolved" from this Java source.
import record.Unresolved;

// This class should be resolved correctly.
public record Record(String string) {}
