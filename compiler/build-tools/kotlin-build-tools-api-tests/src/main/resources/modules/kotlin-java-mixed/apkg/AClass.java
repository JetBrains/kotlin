package apkg;

import bpkg.*;

public class AClass {
    public static String getString() {
        return (new BClass()).X() + "Z";
    }

    public static String getY() {
        return "Y";
    }
}