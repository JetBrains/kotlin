package test;
public record SimpleRecord(int x, CharSequence y) {
    public CharSequence y() { return y; }
    public CharSequence y(int n) { return y; }
    public double z() { return 0.0; }
}
