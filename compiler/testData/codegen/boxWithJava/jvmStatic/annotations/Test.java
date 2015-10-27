import java.lang.String;
import java.lang.annotation.Annotation;

class Test {

    public static String test1() throws NoSuchMethodException {
        Annotation[] test1s = A.class.getMethod("test1").getAnnotations();
        for (Annotation test : test1s) {
            String name = test.toString();
            if (name.contains("testAnnotation")) {
                return "OK";
            }
        }
        return "fail";
    }

    public static String test2() throws NoSuchMethodException {
        Annotation[] test2s = B.class.getMethod("test1").getAnnotations();
        for (Annotation test : test2s) {
            String name = test.toString();
            if (name.contains("testAnnotation")) {
                return "OK";
            }
        }
        return "fail";
    }

}
