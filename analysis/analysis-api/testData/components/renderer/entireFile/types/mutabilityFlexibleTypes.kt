// FILE: main.kt
fun list(j: JavaDeclaration) = j.list()

fun map(j: JavaDeclaration) = j.map()

fun entry(j: JavaDeclaration) = j.entry()

fun rawList(j: JavaDeclaration) = j.rawList()

fun collection(j: JavaDeclaration) = j.collection()

fun notNullList(j: JavaDeclaration) = j.notNullList()

fun nullableList(j: JavaDeclaration) = j.nullableList()

fun iterator(j: JavaDeclaration) = j.iterator()

// FILE: JavaDeclaration.java
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JavaDeclaration {
    List<String> list();

    Map<String, Integer> map();

    Map.Entry<String, Integer> entry();

    List rawList();

    Collection<? extends CharSequence> collection();

    @NotNull
    List<String> notNullList();

    @Nullable
    List<String> nullableList();

    Iterator<String> iterator();
}
