package test;

import java.io.File;

public interface FilenameFilter {
    boolean accept(File dir, String name);
}
