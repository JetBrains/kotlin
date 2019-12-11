//FILE:a/JC.java
package a;

import java.util.Iterator;

public interface JC<T> {
    public Iterator<T> getIterator();

    public void setIterator(Iterator<T> iterator);

    public Iterable<T> getIterable();

    public void setIterable(Iterable<T> iterable);
}

//FILE:n.kt
package n

import a.JC

fun foo(c: JC<Int>, iterator: Iterator<Int>, iterable: Iterable<Int>) {
    val mutableIterator: MutableIterator<Int>? = c.getIterator()
    c.setIterator(mutableIterator)
    c.setIterator(iterator)

    val mutableIterable: MutableIterable<Int>? = c.getIterable()
    c.setIterable(mutableIterable)
    c.setIterable(iterable)
}
