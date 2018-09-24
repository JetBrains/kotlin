
public class Annotated {
    public void foo(@MyNonnull String x) {
    }

    public void bar(@MyMigrationNonnull String x) {
    }

    @MyNullable
    public String nullable() {
        return null;
    }
}
