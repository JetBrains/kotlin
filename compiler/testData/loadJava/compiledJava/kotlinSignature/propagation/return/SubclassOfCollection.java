// JAVAC_EXPECTED_FILE
package test;

import java.util.*;

public interface SubclassOfCollection<E> extends Collection<E> {
    Iterator<E> iterator();

}
