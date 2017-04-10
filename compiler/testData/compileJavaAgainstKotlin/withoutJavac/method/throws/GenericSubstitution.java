package test;

class JavaClass {
    public static void main(String[] args) {
        try {
            new Derived().one(null);
        }
        catch (E1 e) {}
    }
}
