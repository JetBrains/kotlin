package test;

import java.util.AbstractList;

public class ModalityOfFakeOverrides extends AbstractList<String> {
    @Override
    public String get(int index) {
        return "";
    }

    @Override
    public int size() {
        return 0;
    }
}
