package test;

class InnerClassOfGeneric {
    public static void main(String[] args) {
        new Outer<String>().new Inner(new java.util.ArrayList<String>());
        new Outer<String>().new InnerSimple();
    }
}