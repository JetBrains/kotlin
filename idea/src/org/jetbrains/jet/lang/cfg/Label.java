package org.jetbrains.jet.lang.cfg;

/**
 * @author abreslav
 */
public class Label {
    private final String name;

    public Label(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
