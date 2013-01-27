public class funCallInConstructor {

    protected final String protectedProperty;

    public funCallInConstructor(String str) {
        protectedProperty = str;
    }

    protected static String protectedFun() {
        return "OK";
    }
}
