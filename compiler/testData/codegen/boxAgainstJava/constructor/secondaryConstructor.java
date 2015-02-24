package test;

public class secondaryConstructor {
    private final String x;
    private final String y;

    public secondaryConstructor(String x, String y) {
        this.x = x;
        this.y = y;
    }

    public secondaryConstructor(String x) {
        this(x, "def_y");
    }

    public secondaryConstructor() {
        this("def_x");
    }

    @Override
    public String toString() {
        return x + "#" + y;
    }
}
