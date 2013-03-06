package test;

import java.util.Iterator;

public interface LoadIterator<T> {
    public Iterator<T> getIterator();
    public void setIterator(Iterator<T> iterator);
}
