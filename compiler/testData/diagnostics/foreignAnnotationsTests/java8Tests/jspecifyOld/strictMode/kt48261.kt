// FIR_IDENTICAL
// JSR305_GLOBAL_REPORT: strict
// JSPECIFY_STATE: strict
// WITH_STDLIB
// FULL_JDK

// FILE: MyCollection.java
import java.util.AbstractCollection;
import javax.annotation.CheckForNull;
import org.jspecify.nullness.NullMarked;

@NullMarked
public abstract class MyCollection<E> extends AbstractCollection<E> {
    @Override
    public boolean contains(@CheckForNull Object o) {
        return false;
    }
}

// FILE: MyList.java
import java.util.AbstractCollection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.jspecify.nullness.NullMarked;

@NullMarked
public abstract class MyList<E> extends MyCollection<E> implements List<E> {
    @Override
    public boolean contains(@CheckForNull Object o) {
        return false;
    }
}

// FILE: main.kt
fun go(myList : MyList<String>, string : String) = string in myList
