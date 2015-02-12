public class J {
    public static int f() {
       return A.Default.getI1() + A.OBJECT$.getI2() + B.Named.getI1() + B.OBJECT$.getI2();
    }
}
