import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class J extends MyList {
    @Override
    public int getSize() {
        return 55;
    }

    @Override
    public int lastIndexOf(String s) {
        return 0;
    }

    @Override
    public int indexOf(String s) {
        return 0;
    }

    @Override
    public boolean contains(String s) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public String get(int index) {
        return null;
    }

    @Override
    public List<String> subList(int i, int i1) {
        return super.subList(i, i1);
    }

    @Override
    public ListIterator<String> listIterator(int i) {
        return super.listIterator(i);
    }

    @Override
    public ListIterator<String> listIterator() {
        return super.listIterator();
    }
}
