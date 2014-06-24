import java.io.*;

class JavaClass {
    public static String invokeFilter(FileFilter f, File file1, File file2) {
        return f.accept(file1) + " " + f.accept(file2);
    }
}