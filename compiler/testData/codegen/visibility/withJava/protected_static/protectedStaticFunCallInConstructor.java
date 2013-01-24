import java.lang.String;

public class protectedStaticFunCallInConstructor {

    protected final String protectedProperty;

    public protectedStaticFunCallInConstructor(String str) {
        protectedProperty = str;
    }

    protected static String protectedFun() {
        return "OK";
    }
}
