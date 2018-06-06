// FULL_JDK
// JAVAC_EXPECTED_FILE
package test;

import org.jetbrains.annotations.NotNull;
import java.util.AbstractList;

public class ModalityOfFakeOverrides extends AbstractList<String> {
    @Override
    @NotNull
    public String get(int index) {
        return "";
    }

    @Override
    public int size() {
        return 0;
    }
}
