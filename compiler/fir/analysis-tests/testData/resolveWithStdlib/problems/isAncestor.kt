// FILE: their/Condition.java

package their;

public interface Condition<T> {
    boolean value (T t);
}

// FILE: your/ContainerUtil.java

package your;

import their.Condition;

public final class ContainerUtil {
    public static <T> T find(Iterable<? extends T> iterable, Condition<? super T> condition) {
        return true;
    }

    public static <T> T find(Iterable<? extends T> iterable, T equalTo) {
        return true;
    }
}

// FILE: my/FileUtil.java

package my;

import java.io.File;

public final class FileUtil {
    public static boolean isAncestor(File ancestor, File file, boolean strict) {
        return true;
    }

    public static boolean isAncestor(String ancestor, String file, boolean strict) {
        return true;
    }

}

// FILE: test.kt

import java.io.File
import my.FileUtil.*
import your.ContainerUtil.find

fun foo() {
    val externalsMap = mutableMapOf<File, String?>()
    fun test(file: File) {
        val base = find(externalsMap.keys) { isAncestor(it, file, false) }
    }
}
