// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: Pair.java

public class Pair<A, B> {
    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}

// FILE: Couple.java

public class Couple<T> extends Pair<T, T> {
    public Couple(T first, T second) {
        super(first, second);
    }
}

// FILE: FilePath.java

public interface FilePath {
    String getName();
}

// FILE: typeParameterFromJavaClass.kt

fun foo(movedPaths: MutableList<Couple<FilePath>>) {
    movedPaths.forEach { it.second.name }
}
