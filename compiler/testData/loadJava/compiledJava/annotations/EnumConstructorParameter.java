package test;

public enum EnumConstructorParameter {
    INSTANCE("instance");

    public @interface Anno {
        String value();
    }

    EnumConstructorParameter(@Anno("string") String s) {
    }

    EnumConstructorParameter(int x) {
    }
}
