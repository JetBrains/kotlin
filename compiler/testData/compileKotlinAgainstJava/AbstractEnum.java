package test;

public enum AbstractEnum {

    ONE {
        @Override
        String getString() { return null; }
    };

    abstract String getString();

}
