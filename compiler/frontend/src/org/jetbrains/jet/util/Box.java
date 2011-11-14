package org.jetbrains.jet.util;

/**
* @author abreslav
*/
public class Box<T> {
    private final T data;

    public Box(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // This class is needed to screen from calling data's hashCode()
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); // This class is needed to screen from calling data's equals()
    }
}
