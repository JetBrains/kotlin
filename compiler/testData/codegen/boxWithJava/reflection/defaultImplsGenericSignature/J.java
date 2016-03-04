public class J {

    public static int test1() {
        A<String, B<String>> x = new X<String, B<String>>("O", new B<String>("K"));
        return A.DefaultImpls.test1(x, 1, 1.0);
    }


    public static A<String, B<String>> test2(){
        X<String, B<String>> x = new X<String, B<String>>("O", new B<String>("K"));
        return A.DefaultImpls.test2(x, 1);
    }
}