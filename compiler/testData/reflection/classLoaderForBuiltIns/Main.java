import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws Exception {
        URL[] urls = new URL[args.length];
        for (int i = 0; i < args.length; i++) {
            urls[i] = new File(args[i]).toURI().toURL();
        }

        ClassLoader cl = new URLClassLoader(urls);
        Class<?> c = cl.loadClass("TestKt");
        c.getDeclaredMethods()[0].invoke(null);
    }
}
