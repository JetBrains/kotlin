package test;

public enum EnumValues {
    OK(0),
    COMPILATION_ERROR(1),
    INTERNAL_ERROR(2),
    SCRIPT_EXECUTION_ERROR(3);

    private final int code;

    EnumValues(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
