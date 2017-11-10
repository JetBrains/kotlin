import test.SubSeal

public class JavaSubstition {
    public static void main(String[] args) {
        // SubSeal to Variable: OK, just change instanciation to ask for property INSTANCE
        Sealed sealToVar = SubSeal.INSTANCE;

        // SubSeal with method call: OK, just change instanciation to ask for property INSTANCE
        SubSeal.INSTANCE.toString();

        // SubSeal as Expression: Line deleted. you can't use just an expression as a complete statement on Java.
    }
}