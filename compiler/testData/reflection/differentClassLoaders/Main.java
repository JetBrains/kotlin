import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class Main {
    public static void main(String[] args) throws Exception {
        URL[] urls = new URL[args.length];
        for (int i = 0; i < args.length; i++) {
            urls[i] = new File(args[i]).toURI().toURL();
        }
        URLClassLoader classLoader = new URLClassLoader(urls, null);
        Class<?> mainClass = classLoader.loadClass("TestKt");
        Method main = mainClass.getMethod("main", String[].class);
        main.invoke(null, (Object) new String[]{});
    }
}
