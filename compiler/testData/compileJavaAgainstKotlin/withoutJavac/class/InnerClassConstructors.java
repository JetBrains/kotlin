package test;

class InnerClassConstructors {
    public static void main(String[] args) {
        new Outer().new InnerGeneric(new java.util.ArrayList<String>());
        new Outer().new InnerPrimitive(1);
    }
}