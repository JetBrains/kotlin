// FIR_IDENTICAL
// JSR305_GLOBAL_REPORT: strict
// JSPECIFY_STATE: warn
// WITH_STDLIB
// FULL_JDK
// !LANGUAGE: +IgnoreNullabilityForErasedValueParameters

// FILE: MyList.java
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class MyList<E> extends MyCollection<E> implements List<@NotNull E> {
    @Override
    public boolean contains(Object o) {
        return false;
    }
}

// FILE: MyCollection.java
import java.util.AbstractCollection;
import javax.annotation.CheckForNull;

public abstract class MyCollection<E> extends AbstractCollection<E> {
    @Override
    public boolean contains(@CheckForNull Object o) {
        return false;
    }
}

// FILE: main.kt
fun go(myList : MyList<String>) = myList.contains("")