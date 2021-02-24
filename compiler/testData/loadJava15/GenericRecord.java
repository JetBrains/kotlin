package test;
public record GenericRecord<T, E>(T x, E y) {
    public E y() { return y; }
    public E y(E n) { return y; }
    public double z() { return 0.0; }
}
